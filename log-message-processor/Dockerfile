FROM python:3.6-slim

# Instala herramientas necesarias para compilar dependencias de C
RUN apt-get update && apt-get install -y \
    gcc \
    libffi-dev \
    python-dev \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY . .

# Mejora compatibilidad de pip
RUN pip install --upgrade pip setuptools wheel

# Instala dependencias del proyecto
RUN pip install -r requirements.txt

CMD ["python", "main.py"]
