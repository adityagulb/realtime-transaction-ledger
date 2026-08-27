terraform { required_version = ">= 1.6.0" required_providers { aws = { source = "hashicorp/aws" version = "~> 5.0" } random = { source = "hashicorp/random" version = "~> 3.6" } } }
provider "aws" { region = var.aws_region }
resource "random_password" "db" { length = 24 special = true }
resource "aws_db_subnet_group" "ledger" { name = "${var.name}-db-subnets" subnet_ids = var.private_subnet_ids }
resource "aws_security_group" "db" { name = "${var.name}-db" vpc_id = var.vpc_id ingress { from_port=5432 to_port=5432 protocol="tcp" security_groups=var.application_security_group_ids } egress { from_port=0 to_port=0 protocol="-1" cidr_blocks=["0.0.0.0/0"] } }
resource "aws_db_instance" "ledger" { identifier=var.name engine="postgres" engine_version="16" instance_class=var.db_instance_class allocated_storage=20 storage_encrypted=true db_name="ledger" username="ledger_admin" password=random_password.db.result db_subnet_group_name=aws_db_subnet_group.ledger.name vpc_security_group_ids=[aws_security_group.db.id] multi_az=var.multi_az backup_retention_period=7 skip_final_snapshot=true publicly_accessible=false }
output "db_endpoint" { value=aws_db_instance.ledger.address }
output "db_password" { value=random_password.db.result sensitive=true }
