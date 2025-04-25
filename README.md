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
<img width="965" alt="Screenshot 2025-04-23 at 11 30 47 AM" src="https://github.com/user-attachments/assets/bfc02c5d-fced-4908-82d7-46479327fe36" />

Take a look at the components diagram that describes them and their interactions.

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
provider "azurerm" {
  features {}
}

resource "azurerm_resource_group" "rg" {
  name     = var.resource_group_name
  location = var.location
}

resource "azurerm_virtual_network" "vnet" {
  name                = "vnet-devops"
  address_space       = ["10.0.0.0/16"]
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name
}

resource "azurerm_subnet" "subnet" {
  name                 = "subnet-devops"
  resource_group_name  = azurerm_resource_group.rg.name
  virtual_network_name = azurerm_virtual_network.vnet.name
  address_prefixes     = ["10.0.1.0/24"]
}

resource "azurerm_public_ip" "public_ip" {
  name                = "publicip-devops"
  location            = var.location
  resource_group_name = azurerm_resource_group.rg.name
  allocation_method   = "Static"
  sku                 = "Standard" 
}

resource "azurerm_network_interface" "nic" {
  name                = "nic-devops"
  location            = var.location
  resource_group_name = azurerm_resource_group.rg.name

  ip_configuration {
    name                          = "ipconfig1"
    subnet_id                     = azurerm_subnet.subnet.id
    private_ip_address_allocation = "Dynamic"
    public_ip_address_id          = azurerm_public_ip.public_ip.id
  }
}

resource "azurerm_linux_virtual_machine" "vm" {
  name                = "vm-devops"
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
    storage_account_type = "Standard_LRS"
  }

  source_image_reference {
    publisher = "Canonical"
    offer     = "UbuntuServer"
    sku       = "18.04-LTS"
    version   = "latest"
  }
}

resource "azurerm_network_security_group" "nsg" {
  name                = "nsg-devops"
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
}

resource "azurerm_network_interface_security_group_association" "nsg_assoc" {
  network_interface_id      = azurerm_network_interface.nic.id
  network_security_group_id = azurerm_network_security_group.nsg.id
}

```


