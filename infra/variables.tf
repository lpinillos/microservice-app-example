variable "location" {
  default = "East US"
}

variable "resource_group_name" {
  default = "rg-devops-app"
}

variable "admin_username" {
  default = "azureuser"
}

variable "admin_password" {
  description = "Password for the admin user"
  type        = string
  sensitive   = true
}

variable "vm_size" {
  default = "Standard_B1s"
}
