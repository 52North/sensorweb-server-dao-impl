create sequence category_i18n_seq start 1 increment 1;
create sequence category_seq start 1 increment 1;
create sequence dataset_i18n_seq start 1 increment 1;
create sequence dataset_seq start 1 increment 1;
create sequence feature_i18n_seq start 1 increment 1;
create sequence feature_seq start 1 increment 1;
create sequence observation_i18n_seq start 1 increment 1;
create sequence observation_seq start 1 increment 1;
create sequence offering_i18n_seq start 1 increment 1;
create sequence offering_seq start 1 increment 1;
create sequence phenomenon_i18n_seq start 1 increment 1;
create sequence phenomenon_seq start 1 increment 1;
create sequence platform_i18n_seq start 1 increment 1;
create sequence platform_seq start 1 increment 1;
create sequence procedure_i18n_seq start 1 increment 1;
create sequence procedure_seq start 1 increment 1;
create sequence unit_i18n_seq start 1 increment 1;
create sequence unit_seq start 1 increment 1;
create sequence value_profile_i18n_seq start 1 increment 1;
create sequence value_profile_seq start 1 increment 1;

    create table category (
       category_id int8 not null,
        identifier varchar(255) not null,
        name varchar(255),
        description text,
        primary key (category_id)
    );

    create table category_i18n (
       category_i18n_id int8 not null,
        fk_category_id int8 not null,
        locale varchar(255) not null,
        name varchar(255),
        description text,
        primary key (category_i18n_id)
    );

    create table dataset (
       dataset_id int8 not null,
        dataset_type varchar(255) not null check (dataset_type in ('individualObservation', 'timeseries', 'trajectory')),
        observation_type varchar(255) not null check (observation_type in ('simple', 'profile')),
        value_type varchar(255) not null check (value_type in ('quantity', 'count', 'text', 'category', 'bool', 'reference')),
        fk_procedure_id int8 not null,
        fk_phenomenon_id int8 not null,
        fk_offering_id int8 not null,
        fk_category_id int8 not null,
        fk_feature_id int8 not null,
        fk_platform_id int8 not null,
        fk_unit_id int8,
        is_deleted int2 default 0 not null check (is_deleted in (1,0)),
        is_disabled int2 default 0 not null check (is_disabled in (1,0)),
        is_published int2 default 1 not null check (is_published in (1,0)),
        is_mobile int2 default 0 check (is_mobile in (1,0)),
        is_insitu int2 default 1 check (is_insitu in (1,0)),
        is_hidden int2 default 0 not null check (is_hidden in (1,0)),
        origin_timezone varchar(40),
        first_time timestamp with time zone,
        last_time timestamp with time zone,
        first_value numeric(20, 10),
        last_value numeric(20, 10),
        fk_first_observation_id int8,
        fk_last_observation_id int8,
        decimals int4,
        identifier varchar(255),
        name varchar(255),
        description text,
        fk_value_profile_id int8,
        primary key (dataset_id)
    );

    create table dataset_i18n (
       dataset_i18n_id int8 not null,
        fk_dataset_id int8 not null,
        locale varchar(255) not null,
        name varchar(255),
        description text,
        primary key (dataset_i18n_id)
    );

    create table dataset_reference (
       fk_dataset_id_from int8 not null,
        sort_order int4 not null,
        fk_dataset_id_to int8 not null,
        primary key (fk_dataset_id_from, sort_order)
    );

    create table feature (
       feature_id int8 not null,
        discriminator varchar(255),
        identifier varchar(255) not null,
        name varchar(255),
        description text,
        geom GEOMETRY,
        primary key (feature_id)
    );

    create table feature_hierarchy (
       fk_child_feature_id int8 not null,
        fk_parent_feature_id int8 not null,
        primary key (fk_parent_feature_id, fk_child_feature_id)
    );

    create table feature_i18n (
       feature_i18n_id int8 not null,
        fk_feature_id int8 not null,
        locale varchar(255) not null,
        name varchar(255),
        description text,
        primary key (feature_i18n_id)
    );

    create table observation (
       observation_id int8 not null,
        value_type varchar(255) not null,
        fk_dataset_id int8 not null,
        sampling_time_start timestamp with time zone not null,
        sampling_time_end timestamp with time zone not null,
        result_time timestamp with time zone not null,
        identifier varchar(255),
        name varchar(255),
        description text,
        is_deleted int2 default 0 not null check (is_deleted in (1,0)),
        valid_time_start timestamp with time zone default NULL,
        valid_time_end timestamp with time zone default NULL,
        sampling_geometry GEOMETRY,
        value_identifier varchar(255),
        value_name varchar(255),
        value_description varchar(255),
        vertical_from numeric(20, 10) default 0 not null,
        vertical_to numeric(20, 10) default 0 not null,
        fk_parent_observation_id int8,
        value_quantity numeric(20, 10),
        value_text varchar(255),
        value_reference varchar(255),
        value_count int4,
        value_boolean int2,
        value_category varchar(255),
        primary key (observation_id),
        check (value_type in ('quantity', 'count', 'text', 'category', 'bool', 'profile', 'complex', 'dataarray', 'geometry', 'blob', 'reference'))
    );

    create table observation_i18n (
       observation_i18n_id int8 not null,
        fk_observation_id int8 not null,
        locale varchar(255) not null,
        name varchar(255),
        description text,
        value_name varchar(255),
        value_description varchar(255),
        primary key (observation_i18n_id)
    );

    create table offering (
       offering_id int8 not null,
        identifier varchar(255) not null,
        name varchar(255),
        description text,
        sampling_time_start timestamp with time zone,
        sampling_time_end timestamp with time zone,
        result_time_start timestamp with time zone,
        result_time_end timestamp with time zone,
        valid_time_start timestamp with time zone,
        valid_time_end timestamp with time zone,
        geom GEOMETRY,
        primary key (offering_id)
    );

    create table offering_i18n (
       offering_i18n_id int8 not null,
        fk_offering_id int8 not null,
        locale varchar(255) not null,
        name varchar(255),
        description text,
        primary key (offering_i18n_id)
    );

    create table phenomenon (
       phenomenon_id int8 not null,
        identifier varchar(255) not null,
        name varchar(255),
        description text,
        primary key (phenomenon_id)
    );

    create table phenomenon_i18n (
       phenomenon_i18n_id int8 not null,
        fk_phenomenon_id int8 not null,
        locale varchar(255) not null,
        name varchar(255),
        description text,
        primary key (phenomenon_i18n_id)
    );

    create table platform (
       platform_id int8 not null,
        identifier varchar(255) not null,
        name varchar(255),
        description text,
        primary key (platform_id)
    );

    create table platform_i18n (
       platform_i18n_id int8 not null,
        fk_platform_id int8 not null,
        locale varchar(255) not null,
        name varchar(255),
        description text,
        primary key (platform_i18n_id)
    );

    create table "procedure" (
       procedure_id int8 not null,
        identifier varchar(255) not null,
        name varchar(255),
        description text,
        is_reference int2 default 0 check (is_reference in (1,0)),
        primary key (procedure_id)
    );

    create table procedure_i18n (
       procedure_i18n_id int8 not null,
        fk_procedure_id int8 not null,
        locale varchar(255) not null,
        name varchar(255),
        description text,
        short_name varchar(255),
        long_name varchar(255),
        primary key (procedure_i18n_id)
    );

    create table unit (
       unit_id int8 not null,
        symbol varchar(255) not null,
        name varchar(255),
        link varchar(255),
        primary key (unit_id)
    );

    create table unit_i18n (
       unit_i18n_id int8 not null,
        fk_unit_id int8 not null,
        locale varchar(255) not null,
        name varchar(255),
        primary key (unit_i18n_id)
    );

    create table value_profile (
       value_profile_id int8 not null,
        orientation int2,
        vertical_origin_name varchar(255),
        vertical_from_name varchar(255),
        vertical_to_name varchar(255),
        fk_vertical_unit_id int8 not null,
        primary key (value_profile_id)
    );

    create table value_profile_i18n (
       value_profile_i18n_id int8 not null,
        fk_value_profile_id int8 not null,
        locale varchar(255) not null,
        vertical_origin_name varchar(255),
        vertical_from_name varchar(255),
        vertical_to_name varchar(255),
        primary key (value_profile_i18n_id)
    );
create index idx_category_identifier on category (identifier);

    alter table if exists category 
       add constraint un_category_identifier unique (identifier);
create index idx_dataset_dataset_type on dataset (dataset_type);
create index idx_dataset_observation_type on dataset (observation_type);
create index idx_dataset_value_type on dataset (value_type);
create index idx_dataset_identifier on dataset (identifier);

    alter table if exists dataset 
       add constraint un_dataset_identifier unique (identifier);
create index idx_feature_identifier on feature (identifier);

    alter table if exists feature 
       add constraint un_feature_identifier unique (identifier);
create index idx_sampling_time_start on observation (sampling_time_start);
create index idx_sampling_time_end on observation (sampling_time_end);
create index idx_result_time on observation (result_time);

    alter table if exists observation 
       add constraint un_observation_identity unique (value_type, fk_dataset_id, sampling_time_start, sampling_time_end, result_time, vertical_from, vertical_to);

    alter table if exists observation 
       add constraint un_observation_identifier unique (identifier);
create index idx_offering_identifier on offering (identifier);

    alter table if exists offering 
       add constraint un_offering_identifier unique (identifier);
create index idx_phenomenon_identifier on phenomenon (identifier);

    alter table if exists phenomenon 
       add constraint fk_phenomenon_id unique (identifier);
create index idx_platform_identifier on platform (identifier);

    alter table if exists platform 
       add constraint un_platform_identifier unique (identifier);
create index idx_procedure_identifier on "procedure" (identifier);

    alter table if exists "procedure" 
       add constraint un_procedure_identifier unique (identifier);

    alter table if exists unit 
       add constraint un_unit_symbol unique (symbol);

    alter table if exists category_i18n 
       add constraint fk_category 
       foreign key (fk_category_id) 
       references category;

    alter table if exists dataset 
       add constraint fk_dataset_procedure 
       foreign key (fk_procedure_id) 
       references "procedure";

    alter table if exists dataset 
       add constraint fk_dataset_phenomenon 
       foreign key (fk_phenomenon_id) 
       references phenomenon;

    alter table if exists dataset 
       add constraint fk_dataset_offering 
       foreign key (fk_offering_id) 
       references offering;

    alter table if exists dataset 
       add constraint fk_dataset_category 
       foreign key (fk_category_id) 
       references category;

    alter table if exists dataset 
       add constraint fk_dataset_feature 
       foreign key (fk_feature_id) 
       references feature;

    alter table if exists dataset 
       add constraint fk_dataset_platform 
       foreign key (fk_platform_id) 
       references platform;

    alter table if exists dataset 
       add constraint fk_dataset_unit 
       foreign key (fk_unit_id) 
       references unit;

    alter table if exists dataset 
       add constraint fk_dataset_first_obs 
       foreign key (fk_first_observation_id) 
       references observation;

    alter table if exists dataset 
       add constraint fk_dataset_last_obs 
       foreign key (fk_last_observation_id) 
       references observation;

    alter table if exists dataset 
       add constraint fk_value_profile 
       foreign key (fk_value_profile_id) 
       references value_profile;

    alter table if exists dataset_i18n 
       add constraint fk_dataset_i18n 
       foreign key (fk_dataset_id) 
       references dataset;

    alter table if exists dataset_reference 
       add constraint fk_dataset_reference_to 
       foreign key (fk_dataset_id_to) 
       references dataset;

    alter table if exists dataset_reference 
       add constraint fk_dataset_reference_from 
       foreign key (fk_dataset_id_from) 
       references dataset;

    alter table if exists feature_hierarchy 
       add constraint fk_feature_parent 
       foreign key (fk_parent_feature_id) 
       references feature;

    alter table if exists feature_hierarchy 
       add constraint fk_feature_child 
       foreign key (fk_child_feature_id) 
       references feature;

    alter table if exists feature_i18n 
       add constraint fk_feature 
       foreign key (fk_feature_id) 
       references feature;

    alter table if exists observation 
       add constraint fk_dataset 
       foreign key (fk_dataset_id) 
       references dataset;

    alter table if exists observation 
       add constraint fk_parent_observation 
       foreign key (fk_parent_observation_id) 
       references observation;

    alter table if exists observation_i18n 
       add constraint fk_observation_i18n 
       foreign key (fk_observation_id) 
       references observation;

    alter table if exists offering_i18n 
       add constraint fk_offering 
       foreign key (fk_offering_id) 
       references offering;

    alter table if exists phenomenon_i18n 
       add constraint fk_phenomenon 
       foreign key (fk_phenomenon_id) 
       references phenomenon;

    alter table if exists platform_i18n 
       add constraint fk_platform 
       foreign key (fk_platform_id) 
       references platform;

    alter table if exists procedure_i18n 
       add constraint fk_i18n_procedure 
       foreign key (fk_procedure_id) 
       references "procedure";

    alter table if exists unit_i18n 
       add constraint fk_unit 
       foreign key (fk_unit_id) 
       references unit;

    alter table if exists value_profile 
       add constraint fk_profile_unit 
       foreign key (fk_vertical_unit_id) 
       references unit;

    alter table if exists value_profile_i18n 
       add constraint fk_value_profile_i18n 
       foreign key (fk_value_profile_id) 
       references value_profile;
