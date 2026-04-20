import streamlit as st
import requests
from PIL import Image
import psycopg2
import pandas as pd

API_URL = "http://localhost:9001/predict"
DB_CONFIG = {
    "host": "localhost",
    "database": "weather_db",
    "user": "postgres",
    "password": "1234"
}

st.set_page_config(page_title="Weather Recognition", page_icon="🌤", layout="wide")

st.markdown("""
<style>
    .main-title {
        font-size: 2.5rem;
        font-weight: 700;
        text-align: center;
        margin-bottom: 0;
    }
    .sub-title {
        font-size: 1.1rem;
        text-align: center;
        color: gray;
        margin-bottom: 2rem;
    }
    .metric-card {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        padding: 1.5rem;
        border-radius: 1rem;
        color: white;
        text-align: center;
    }
    .result-label {
        font-size: 2rem;
        font-weight: 700;
        text-align: center;
    }
</style>
""", unsafe_allow_html=True)

st.markdown('<p class="main-title">Weather Image Recognition</p>', unsafe_allow_html=True)
st.markdown('<p class="sub-title">Powered by ResNet50 - Transfer Learning | Accuracy: 98%</p>', unsafe_allow_html=True)

with st.sidebar:
    st.header("A propos du projet")
    st.write("**Module :** Scala")
    st.write("**Dataset :** Kaggle Weather Images")
    st.write("**Modele :** ResNet50 (Transfer Learning)")
    st.write("**Accuracy :** 98%")
    st.write("**Classes :** 11 types de meteo")
    st.divider()
    st.write("**Architecture :**")
    st.write("1. Microservice 1 (Scala) - Preprocessing")
    st.write("2. Notebook Python - Training")
    st.write("3. Microservice 2 - Prediction + BDD")
    st.write("4. Streamlit - Interface")
    st.divider()
    st.write("**Categories :**")
    categories = ["dew", "fogsmog", "frost", "glaze", "hail",
                  "lightning", "rain", "rainbow", "rime", "sandstorm", "snow"]
    for cat in categories:
        st.write(f"  - {cat}")

tab1, tab2, tab3 = st.tabs(["Prediction", "Historique", "Statistiques"])

with tab1:
    uploaded_file = st.file_uploader("Choisis une image", type=["jpg", "jpeg", "png"])

    if uploaded_file is not None:
        col1, col2 = st.columns(2)

        with col1:
            st.subheader("Image uploadee")
            image = Image.open(uploaded_file)
            st.image(image, use_container_width=True)

        with col2:
            st.subheader("Resultat")

            with st.spinner("Analyse en cours..."):
                uploaded_file.seek(0)
                files = {
                    "file": (uploaded_file.name, uploaded_file.getvalue(), uploaded_file.type)
                }
                response = requests.post(API_URL, files=files)

            if response.status_code == 200:
                result = response.json()
                label = result["label"]
                confidence = result["confidence"]
                predictions = result["predictions"]

                st.markdown(f'<p class="result-label">{label.upper()}</p>', unsafe_allow_html=True)

                col_a, col_b = st.columns(2)
                col_a.metric("Categorie", label)
                col_b.metric("Confiance", f"{confidence}%")

                st.divider()
                st.write("**Detail des predictions :**")
                sorted_preds = sorted(predictions.items(), key=lambda x: x[1], reverse=True)

                for weather, prob in sorted_preds[:5]:
                    st.progress(min(prob / 100, 1.0), text=f"{weather}: {prob}%")

                st.success("Prediction reussie")
            else:
                st.error(f"Erreur API : {response.status_code}")
                st.text(response.text)

with tab2:
    st.subheader("Historique des predictions")
    try:
        conn = psycopg2.connect(**DB_CONFIG)
        cur = conn.cursor()
        cur.execute("""
            SELECT image_name, predicted_label, confidence, model_name, created_at
            FROM predictions
            ORDER BY created_at DESC
            LIMIT 20
        """)
        rows = cur.fetchall()
        cur.close()
        conn.close()

        if rows:
            df_hist = pd.DataFrame(rows, columns=["Image", "Prediction", "Confiance %", "Modele", "Date"])
            st.dataframe(df_hist, use_container_width=True)
        else:
            st.write("Aucune prediction pour le moment")
    except Exception as e:
        st.warning(f"Erreur : {e}")

with tab3:
    st.subheader("Statistiques des predictions")
    try:
        conn = psycopg2.connect(**DB_CONFIG)
        cur = conn.cursor()

        cur.execute("SELECT COUNT(*) FROM predictions")
        total = cur.fetchone()[0]

        cur.execute("""
            SELECT predicted_label, COUNT(*), AVG(confidence)
            FROM predictions
            GROUP BY predicted_label
            ORDER BY COUNT(*) DESC
        """)
        stats = cur.fetchall()

        cur.close()
        conn.close()

        st.metric("Total predictions", total)

        if stats:
            df_stats = pd.DataFrame(stats, columns=["Categorie", "Nombre", "Confiance moyenne %"])

            col1, col2 = st.columns(2)
            with col1:
                st.bar_chart(df_stats.set_index("Categorie")["Nombre"])
            with col2:
                st.dataframe(df_stats, use_container_width=True)
    except Exception as e:
        st.warning(f"Erreur : {e}")