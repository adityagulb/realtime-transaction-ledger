variable "aws_region" { type=string default="ap-south-1" }
variable "name" { type=string default="case1-ledger" }
variable "vpc_id" { type=string }
variable "private_subnet_ids" { type=list(string) }
variable "application_security_group_ids" { type=list(string) default=[] }
variable "db_instance_class" { type=string default="db.t4g.micro" }
variable "multi_az" { type=bool default=false }
