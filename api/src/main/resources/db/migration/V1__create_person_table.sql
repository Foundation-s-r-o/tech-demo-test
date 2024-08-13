create table PERSON (
	`ID` bigint not null auto_increment, 
	`FIRST_NAME` varchar(255) not null, 
	`LAST_NAME` varchar(255) not null, 
	`EMAIL` varchar(255) not null, 
	`ADDRESS` varchar(255), 
	`STATE` varchar(255), 
	`PHONE_NUMBER` varchar(255), 
	primary key (ID)
) engine=InnoDB;

create index IDX_LN_FN on PERSON (LAST_NAME asc, FIRST_NAME asc);

alter table PERSON add constraint UK_EMAIL unique (EMAIL);