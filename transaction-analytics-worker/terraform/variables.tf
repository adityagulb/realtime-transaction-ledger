variable "aws_region" { type=string default="ap-south-1" }
variable "name" { type=string default="case1-events" }
variable "vpc_id" { type=string }
variable "private_subnet_ids" { type=list(string) }
variable "application_security_group_ids" { type=list(string) default=[] }
