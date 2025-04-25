# Microservice App - PRFT Devops Training

This is the application you are going to use through the whole traninig. This, hopefully, will teach you the fundamentals you need in a real project. You will find a basic TODO application designed with a [microservice architecture](https://microservices.io). Although is a TODO application, it is interesting because the microservices that compose it are written in different programming language or frameworks (Go, Python, Vue, Java, and NodeJS). With this design you will experiment with multiple build tools and environments. 

## Components
In each folder you can find a more in-depth explanation of each component:

1. [Users API](/users-api) is a Spring Boot application. Provides user profiles. At the moment, does not provide full CRUD, just getting a single user and all users.
2. [Auth API](/auth-api) is a Go application, and provides authorization functionality. Generates [JWT](https://jwt.io/) tokens to be used with other APIs.
3. [TODOs API](/todos-api) is a NodeJS application, provides CRUD functionality over user's TODO records. Also, it logs "create" and "delete" operations to [Redis](https://redis.io/) queue.
4. [Log Message Processor](/log-message-processor) is a queue processor written in Python. Its purpose is to read messages from a Redis queue and print them to standard output.
5. [Frontend](/frontend) Vue application, provides UI.

## Architecture
<img width="1192" alt="Screenshot 2025-04-25 at 11 22 10 AM" src="https://github.com/user-attachments/assets/640eab1a-0209-4c9d-8d45-1cd119f4ec28" />

Take a look at the components diagram that describes them and their interactions.

## Deployment
- Frontend ``` http://172.191.101.246:8081/#/login?redirect=%2F ```
- Zipkin ``` http://172.191.101.246:9411/zipkin/#/  ```
- Prometheus ``` http://172.191.101.246:9090/targets ```
- Grafana ```http://172.191.101.246:3000/login ```


## Workflows

The following GitHub Actions workflows are configured:

- `auth-api.yml`
- `deploy-app.yml`
- `deploy-infra.yml`
- `frontend.yml`
- `log-message-processor.yml`
- `todos-api.yml`
- `users-api.yml`

## Agile Methodology

We follow a Scrum-based development approach. You can find the board [here](https://docs.google.com/spreadsheets/d/1xYkQaqoBBeor4f1m7Yfc0LKtROHPU7GbzGJ4xlwMVnM/edit?usp=sharing).

## Cloud Design Patterns Implemented

- **Bulkhead**
- **Cache-Aside**
- **Circuit Breaker**
- **Health Endpoint Monitoring**

## Observability

Integrated with **Grafana** and **Prometheus**.

**Prometheus scrape config:**
```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'node-exporter'
    static_configs:
      - targets: ['host.docker.internal:9100']

  - job_name: 'users-api'
    metrics_path: /healthz
    static_configs:
      - targets: ['users-api:8083']

  - job_name: 'circuit-breaker'
    metrics_path: /circuitz
    static_configs:
      - targets: ['users-api:8083']
```

Circuit Breaker metrics endpoint: Custom endpoint /circuitz exposes Resilience4j circuit breaker stats as Prometheus metrics.

### Docker

This project includes containerized versions of all microservices using Docker. Each service has its own Dockerfile, and the entire system can be launched using ```docker-compose```.

### Containerized Microservices Overview

| Service         | Language/Stack     | Dockerfile Path                        | Exposed Port |
|----------------|--------------------|----------------------------------------|--------------|
| `auth-api`     | Go                 | `./auth-api/Dockerfile`               | 8000         |
| `users-api`    | Java (Spring Boot) | `./users-api/Dockerfile`              | 8083         |
| `todos-api`    | Node.js            | `./todos-api/Dockerfile`              | 8082         |
| `log-processor`| Python             | `./log-message-processor/Dockerfile`  | N/A          |
| `frontend`     | Vue.js             | `./frontend/Dockerfile`               | 8081         |
| `redis`        | Redis              | *(Docker Hub official image)*         | 6379         |




Running Everything (Production or Dev)
From the root of the project, use:

```bash
docker-compose up --build
```
This will:

Build and run all services

Link them via Docker internal networking

Start the frontend at ```http://localhost:8081```

 Environment Variables (passed via Compose)
Each service is configured through environment variables defined in ```docker-compose.yml```. 

Example:

```
auth-api:
  environment:
    - AUTH_API_PORT=8000
    - USERS_API_ADDRESS=http://users-api:8083
    - JWT_SECRET=PRFT
```
Cleaning Containers
To stop and remove all containers:

```docker-compose down```



## Infraestructure

Terraform

```
// main
provider "azurerm" {
  features {}
}

resource "azurerm_resource_group" "rg" {
  name     = var.resource_group_name
  location = var.location
}

resource "azurerm_virtual_network" "vnet" {
  name                = var.vnet_name
  address_space       = var.vnet_address_space
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name
}

resource "azurerm_subnet" "subnet" {
  name                 = var.subnet_name
  resource_group_name  = azurerm_resource_group.rg.name
  virtual_network_name = azurerm_virtual_network.vnet.name
  address_prefixes     = var.subnet_address_prefixes
}

resource "azurerm_public_ip" "public_ip" {
  name                = var.public_ip_name
  location            = var.location
  resource_group_name = azurerm_resource_group.rg.name
  allocation_method   = var.public_ip_allocation_method
  sku                 = var.public_ip_sku
}

resource "azurerm_network_interface" "nic" {
  name                = var.nic_name
  location            = var.location
  resource_group_name = azurerm_resource_group.rg.name

  ip_configuration {
    name                          = "ipconfig1"
    subnet_id                     = azurerm_subnet.subnet.id
    private_ip_address_allocation = "Static"
    private_ip_address            = "10.0.1.4"
    public_ip_address_id          = azurerm_public_ip.public_ip.id
  }
}

resource "azurerm_linux_virtual_machine" "vm" {
  name                = var.vm_name
  resource_group_name = azurerm_resource_group.rg.name
  location            = var.location
  size                = var.vm_size
  admin_username      = var.admin_username
  admin_password      = var.admin_password
  disable_password_authentication = false
  network_interface_ids = [
    azurerm_network_interface.nic.id,
  ]

  os_disk {
    caching              = "ReadWrite"
    storage_account_type = var.storage_account_type
  }

  source_image_reference {
    publisher = var.image_publisher
    offer     = var.image_offer
    sku       = var.image_sku
    version   = var.image_version
  }

    provision_vm_agent = true
    custom_data        = base64encode(file("config.sh"))
}

resource "azurerm_network_security_group" "nsg" {
  name                = var.nsg_name
  location            = var.location
  resource_group_name = azurerm_resource_group.rg.name

  security_rule {
    name                       = "Allow-SSH"
    priority                   = 1001
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "22"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }

  security_rule {
    name                       = "port-8000"
    priority                   = 1002
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "8000"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }

  security_rule {
    name                       = "port-8081"
    priority                   = 1003
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "8081"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }

  security_rule {
    name                       = "port-8082"
    priority                   = 1004
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "8082"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }

  security_rule {
    name                       = "port-8083"
    priority                   = 1005
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "8083"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }
}

resource "azurerm_network_interface_security_group_association" "nsg_assoc" {
  network_interface_id      = azurerm_network_interface.nic.id
  network_security_group_id = azurerm_network_security_group.nsg.id
}

terraform {
  backend "azurerm" {
    resource_group_name  = "devops-workshop"
    storage_account_name = "tfstatestorage123lp"
    container_name       = "tfstatelp"
    key                  = "terraform.tfstate"
  }
}

```


```markdown


---

## Estructura del Proyecto

```bash
.
├── auth-api/                # Servicio en Go
├── todos-api/              # Servicio en Node.js
├── users-api/              # Servicio en Java (Maven)
├── frontend/               # Aplicación frontend en Node.js
├── log-message-processor/  # Procesador de logs en Python
├── infra/                  # Código de infraestructura en Terraform
├── .github/
│   └── workflows/          # Workflows de GitHub Actions
```

---

## Workflows de CI/CD

### 1. Auth-api

- **Lenguaje:** Go (v1.18)
- **Disparador:** Push o PR en `auth-api/**`
- **Acciones:**
  - Compilación del servicio
  - Ejecución de pruebas de disponibilidad

```yaml
go build -o main .
./main & curl -I http://localhost:8000
```

---

### 2. Todos-api

- **Lenguaje:** Node.js (v8.17.0)
- **Disparador:** Push o PR en `todos-api/**`
- **Acciones:**
  - Instalación de dependencias
  - Ejecución de tests si están definidos en `package.json`

---

### 3. Users-api

- **Lenguaje:** Java (JDK 8, Maven)
- **Disparador:** Push o PR en `users-api/**`
- **Acciones:**
  - Compilación con Maven (`./mvnw clean install`)

---

### 4. Frontend

- **Lenguaje:** Node.js (v8.17.0)
- **Disparador:** Push o PR en `frontend/**`
- **Acciones:**
  - Instalación y construcción de la app
  - Ejecución local para pruebas de disponibilidad

```bash
npm run build
PORT=8081 npm start & curl -I http://localhost:8081
```

---

### 5. Log Message Processor (Python)

- **Lenguaje:** Python 3.10
- **Disparador:** Push en `log-message-processor/**`
- **Acciones:**
  - Instalación de dependencias
  - Verificación de sintaxis (`python -m py_compile`)

---

### 6. Deploy Infrastructure (Terraform)

- **Disparador:** Push en `infra/**`
- **Acciones:**
  - `terraform init` y `terraform apply` con credenciales desde secretos

```yaml
working-directory: infra
```

- **Variables de entorno necesarias:**
  - `ARM_CLIENT_ID`
  - `ARM_CLIENT_SECRET`
  - `ARM_SUBSCRIPTION_ID`
  - `ARM_TENANT_ID`

---

### 7. Continuous Deployment a VM

- **Disparador:** Push a rama `master`
- **Acciones:**
  - Conexión SSH a la VM usando `sshpass`
  - Detención de contenedores existentes
  - Clonado y reconstrucción del repositorio en la VM

```bash
sudo docker-compose down
git clone ...
sudo docker-compose up -d --build
```

- **Requiere el secreto:** `VM_PASSWORD`

---

## Requerimientos

- GitHub Actions activado en el repositorio.
- Docker y Docker Compose en la máquina destino.
- Infraestructura soportada por Azure (ARM).
- Repositorio bien estructurado por microservicio.
- Test scripts definidos donde aplique (`npm test`, `go test`, etc.).

---

## Secretos Requeridos

Se deben definir los siguientes secretos en la configuración del repositorio:

| Nombre del Secreto       | Uso en Workflow                        |
|--------------------------|----------------------------------------|
| `VM_PASSWORD`            | Acceso remoto vía SSH a la VM          |
| `ARM_CLIENT_ID`          | Terraform (Azure)                      |
| `ARM_CLIENT_SECRET`      | Terraform (Azure)                      |
| `ARM_SUBSCRIPTION_ID`    | Terraform (Azure)                      |
| `ARM_TENANT_ID`          | Terraform (Azure)                      |

---
