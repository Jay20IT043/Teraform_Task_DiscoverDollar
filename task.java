//Write a Terraform script to create a
//
//        VPC/VNET
//        Create one Private Subnet & Public Subnet
//        Deploy Cloud NAT and enable NATing for Private subnet
//        Deploy Virtual machine on Private Subnet and check the internet access
//        Go to VM in Private Subnet and Check what is my IP, it should display NAT Public IP

# Define variables
        variable "project_id" {}
        variable "region" {}
        variable "subnet_cidr_private" {}
        variable "subnet_cidr_public" {}

        # Create VPC
        resource "google_compute_network" "vpc_network" {
        name                    = "my-vpc"
        auto_create_subnetworks = false
        }

        # Create private subnet
        resource "google_compute_subnetwork" "private_subnet" {
        name          = "private-subnet"
        network       = google_compute_network.vpc_network.self_link
        ip_cidr_range = var.subnet_cidr_private
        region        = var.region
        }

        # Create public subnet
        resource "google_compute_subnetwork" "public_subnet" {
        name          = "public-subnet"
        network       = google_compute_network.vpc_network.self_link
        ip_cidr_range = var.subnet_cidr_public
        region        = var.region
        access_config {
        nat_ip = google_compute_router_nat.public_nat_router_nat_ip.self_link
        }
        }

        # Create Cloud NAT
        resource "google_compute_router_nat" "public_nat_router" {
        name               = "public-nat-router"
        router             = google_compute_router.router.self_link
        nat_ip_allocate_option = "AUTO_ONLY"
        }

        # Create a router interface for the NAT
        resource "google_compute_router_nat" "public_nat_router_interface" {
        name       = "public-nat-router-interface"
        router     = google_compute_router.router.self_link
        nat_ip_address = google_compute_router_nat.public_nat_router_nat_ip.self_link
        subnetwork = google_compute_subnetwork.public_subnet.self_link
        }

        # Create virtual machine on private subnet
        resource "google_compute_instance" "vm_instance" {
        name         = "vm-instance"
        machine_type = "e2-medium"
        zone         = "${var.region}-a"

        boot_disk {
        initialize_params {
        image = "debian-cloud/debian-10"
        }
        }

        network_interface {
        subnetwork = google_compute_subnetwork.private_subnet.self_link
        }

        metadata_startup_script = "sudo apt-get update && sudo apt-get install -y apache2 && sudo service apache2 start"
        }

        # Output NAT public IP
        output "nat_public_ip" {
        value = google_compute_router_nat.public_nat_router_nat_ip.address
        }
