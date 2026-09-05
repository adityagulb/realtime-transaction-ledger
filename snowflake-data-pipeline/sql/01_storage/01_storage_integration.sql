-- Replace BOTH placeholders before running.
-- Do not commit AWS secret/access keys.

CREATE OR REPLACE STORAGE INTEGRATION BANKING_S3_INT
  TYPE = EXTERNAL_STAGE
  STORAGE_PROVIDER = 'S3'
  ENABLED = TRUE
  STORAGE_AWS_ROLE_ARN = '<YOUR_AWS_ROLE_ARN>'
  STORAGE_ALLOWED_LOCATIONS = ('s3://<YOUR_S3_BUCKET>/banking-transactions/');

-- After creation, run this and update the AWS IAM role trust policy
-- with STORAGE_AWS_IAM_USER_ARN and STORAGE_AWS_EXTERNAL_ID.
DESC INTEGRATION BANKING_S3_INT;
