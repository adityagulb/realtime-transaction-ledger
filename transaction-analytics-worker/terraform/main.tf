terraform { required_version=">= 1.6.0" required_providers { aws={source="hashicorp/aws" version="~> 5.0"} } }
provider "aws" { region=var.aws_region }
resource "aws_security_group" "msk" { name="${var.name}-msk" vpc_id=var.vpc_id ingress { from_port=9098 to_port=9098 protocol="tcp" security_groups=var.application_security_group_ids } egress { from_port=0 to_port=0 protocol="-1" cidr_blocks=["0.0.0.0/0"] } }
resource "aws_msk_serverless_cluster" "events" { cluster_name=var.name client_authentication { sasl { iam { enabled=true } } } vpc_config { subnet_ids=var.private_subnet_ids security_group_ids=[aws_security_group.msk.id] } }
output "bootstrap_brokers_sasl_iam" { value=aws_msk_serverless_cluster.events.bootstrap_brokers_sasl_iam }
