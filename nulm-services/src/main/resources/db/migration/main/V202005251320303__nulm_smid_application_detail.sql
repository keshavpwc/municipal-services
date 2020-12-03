CREATE TABLE nulm_smid_application_detail (
	application_uuid varchar(64) NOT NULL,
	application_id varchar(64) NOT NULL,
	nulm_application_id varchar(64) NULL,
	application_status varchar(64) NULL,
	"name" varchar(255) NULL,
	position_level varchar(255) NULL,
	gender varchar(255) NULL,
	dob timestamp NULL,
	date_of_opening_account timestamp NULL,
	adhar_no varchar(20) NULL,
	mother_name varchar(255) NULL,
	father_or_husband_name varchar(255) NULL,
	address varchar(255) NULL,
	phone_no varchar(255) NULL,
	mobile_no varchar(255) NULL,
	qualification varchar(255) NULL,
	email_id varchar(255) NULL,
	is_urban_poor bool NULL,
	is_minority bool NULL,
	is_pwd bool NULL,
	is_street_vendor bool NULL,
	is_homeless bool NULL,
	is_insurance bool NULL,
	bpl_no varchar(255) NULL,
	minority varchar(255) NULL,
	caste varchar(255) NULL,
	ward_no varchar(255) NULL,
	name_as_per_adhar varchar(255) NULL,
	adhar_acknowledgement_no varchar(255) NULL,
	insurance_through varchar(255) NULL,
	account_no varchar(255) NULL,
	bank_name varchar(255) NULL,
	branch_name varchar(255) NULL,
	account_opened_through varchar(255) NULL,
	ro_type varchar(255) NULL,
	tenant_id varchar(256) NULL,
	is_active bool NULL,
	created_by varchar(64) NULL,
	created_time int8 NULL,
	last_modified_by varchar(64) NULL,
	last_modified_time int8 NULL,
	remark varchar(256) NULL,
	document_attachemnt jsonb NULL,
	is_registered bool NULL,
	cob_number varchar(255) NULL,
	CONSTRAINT nulm_smid_application_detail_pkey PRIMARY KEY (application_uuid),
	CONSTRAINT smid_application_id UNIQUE (application_id)
);