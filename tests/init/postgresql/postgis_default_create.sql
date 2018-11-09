create sequence category_i18n_seq start 1 increment 1;
create sequence category_seq start 1 increment 1;
create sequence codespace_seq start 1 increment 1;
create sequence data_i18n_seq start 1 increment 1;
create sequence dataset_seq start 1 increment 1;
create sequence feature_i18n_seq start 1 increment 1;
create sequence feature_seq start 1 increment 1;
create sequence format_seq start 1 increment 1;
create sequence observation_seq start 1 increment 1;
create sequence offering_i18n_seq start 1 increment 1;
create sequence offering_seq start 1 increment 1;
create sequence parameter_seq start 1 increment 1;
create sequence phenomenon_i18n_seq start 1 increment 1;
create sequence phenomenon_seq start 1 increment 1;
create sequence procedure_history_seq start 1 increment 1;
create sequence procedure_i18n_seq start 1 increment 1;
create sequence procedure_seq start 1 increment 1;
create sequence related_feature_seq start 1 increment 1;
create sequence result_template_seq start 1 increment 1;
create sequence service_seq start 1 increment 1;
create sequence unit_i18n_seq start 1 increment 1;
create sequence unit_seq start 1 increment 1;

    create table category (
       category_id int8 not null,
        identifier varchar(255) not null,
        name varchar(255),
        description varchar(255),
        primary key (category_id)
    );

    comment on table category is
        'Storage of the categories.';

    comment on column category.category_id is
        'PK column of the table';

    comment on column category.identifier is
        'Unique identifier of the category which can be for filtering, e.g. in the SOS.';

    comment on column category.name is
        'The human readable name of the category.';

    comment on column category.description is
        'A short description of the category';

    create table category_i18n (
       category_i18n_id int8 not null,
        fk_category_id int8 not null,
        locale varchar(255) not null,
        name varchar(255),
        description varchar(255),
        primary key (category_i18n_id)
    );

    comment on table category_i18n is
        'Storage for internationalizations of categories.';

    comment on column category_i18n.category_i18n_id is
        'PK column of the table';

    comment on column category_i18n.fk_category_id is
        'Reference to the category table this internationalization belongs to.';

    comment on column category_i18n.locale is
        'Locale/language specification for this entry';

    comment on column category_i18n.name is
        'Locale/language specific name of the category';

    comment on column category_i18n.description is
        'Locale/language specific description of the category';

    create table codespace (
       codespace_id int8 not null,
        name varchar(255) not null,
        primary key (codespace_id)
    );

    comment on table codespace is
        'Storage of codespaces which can be domain specific.';

    comment on column codespace.codespace_id is
        'PK column of the table';

    comment on column codespace.name is
        'Name/definition of the codespace, e.g. of a domain';

    create table composite_observation (
       fk_parent_observation_id int8 not null,
        fk_child_observation_id int8 not null,
        primary key (fk_parent_observation_id, fk_child_observation_id)
    );

    comment on table composite_observation is
        'Storage of the relation of composite data/observation like Complex(Record)-, Profile- or DataArrayObservation ';

    comment on column composite_observation.fk_parent_observation_id is
        'Reference to the parent data/observation';

    comment on column composite_observation.fk_child_observation_id is
        'Reference to the child data/observation';

    create table composite_phenomenon (
       fk_child_phenomenon_id int8 not null,
        fk_parent_phenomenon_id int8 not null,
        primary key (fk_parent_phenomenon_id, fk_child_phenomenon_id)
    );

    comment on table composite_phenomenon is
        'Storage of hierarchies between phenomenon, e.g. for composite phenomenon like weather with temperature, windspeed, ...';

    comment on column composite_phenomenon.fk_child_phenomenon_id is
        'Reference to the child phenomenon in phenomenon table.';

    comment on column composite_phenomenon.fk_parent_phenomenon_id is
        'Reference to the parent phenomenon in phenomenon table.';

    create table dataset (
       dataset_id int8 not null,
        value_type varchar(255) not null,
        fk_procedure_id int8 not null,
        fk_phenomenon_id int8 not null,
        fk_offering_id int8 not null,
        fk_category_id int8,
        fk_feature_id int8,
        fk_format_id int8,
        first_time timestamp with time zone,
        last_time timestamp with time zone,
        first_value numeric(19, 2),
        last_value numeric(19, 2),
        is_deleted int2 default 0 not null check (is_deleted in (1,0)),
        is_disabled int2 default 0 not null check (is_disabled in (1,0)),
        is_published int2 default 1 not null check (is_published in (1,0)),
        is_mobile int2 default 0 check (is_mobile in (1,0)),
        is_insitu int2 default 1 check (is_insitu in (1,0)),
        origin_timezome varchar(255),
        is_hidden int2 default 0 not null check (is_hidden in (1,0)),
        identifier varchar(255),
        fk_identifier_codespace_id int8,
        name varchar(255),
        fk_name_codespace_id int8,
        description varchar(255),
        fk_unit_id int8,
        fk_first_observation_id int8,
        fk_last_observation_id int8,
        decimals int4,
        primary key (dataset_id),
        check (value_type in ('quantity', 'count', 'text', 'category', 'boolean', 'quantity-profile', 'text-profile', 'category-profile', 'complex', 'dataarray', 'geometry', 'blob', 'referenced', 'not_initialized'))
    );

    comment on table dataset is
        'Storage of the dataset, the core table of the whole database model.';

    comment on column dataset.dataset_id is
        'PK column of the table';

    comment on column dataset.value_type is
        'Indicator used by Hibernate to map value specific entities.';

    comment on column dataset.fk_procedure_id is
        'Reference to the procedure that belongs that belongs to this dataset.';

    comment on column dataset.fk_phenomenon_id is
        'Reference to the phenomenon that belongs that belongs to this dataset.';

    comment on column dataset.fk_offering_id is
        'Reference to the offering that belongs that belongs to this dataset.';

    comment on column dataset.fk_category_id is
        'Reference to the category that belongs that belongs to this dataset.';

    comment on column dataset.fk_feature_id is
        'Reference to the feature that belongs that belongs to this dataset.';

    comment on column dataset.fk_format_id is
        'Reference to the observationType in the format table. Required by the SOS to persist the valid observationType for the dataset.';

    comment on column dataset.first_time is
        'The timestamp of the temporally first data/observation that belongs to this dataset.';

    comment on column dataset.last_time is
        'The timestamp of the temporally last data/observation that belongs to this dataset.';

    comment on column dataset.first_value is
        'The value of the temporally first data/observation that belongs to this dataset.';

    comment on column dataset.last_value is
        'The value of the temporally last quantity data/observation that belongs to this dataset.';

    comment on column dataset.is_deleted is
        'Flag that indicates if this dataset is deleted';

    comment on column dataset.is_disabled is
        'Flag that indicates if this dataset is disabled for insertion of new data';

    comment on column dataset.is_published is
        'Flag that indicates if this dataset should be published';

    comment on column dataset.is_mobile is
        'Flag that indicates if the procedure is mobile (1/true) or stationary (0/false).';

    comment on column dataset.is_insitu is
        'Flag that indicates if the procedure is insitu (1/true) or remote (0/false).';

    comment on column dataset.origin_timezome is
        'Define the origin timezone of the dataset timestamps. Possible values are offset (+02:00), id (CET) or full name (Europe/Berlin)';

    comment on column dataset.is_hidden is
        'Flag that indicates if this dataset should be hidden, e.g. for sub-datasets of a complex datasets';

    comment on column dataset.identifier is
        'Unique identifier of the dataset which can be for filtering, e.g. GetObservationById in the SOS';

    comment on column dataset.fk_identifier_codespace_id is
        'The codespace of the dataset identifier, reference to the codespace table.';

    comment on column dataset.name is
        'The human readable name of the dataset.';

    comment on column dataset.fk_name_codespace_id is
        'The codespace of the dataset name, reference to the codespace table.';

    comment on column dataset.description is
        'A short description of the dataset';

    comment on column dataset.fk_unit_id is
        'Reference to the unit of the data/observations that belongs to this dataset.';

    comment on column dataset.fk_first_observation_id is
        'Reference to the temporally first data/observation in the observation table that belongs to this dataset.';

    comment on column dataset.fk_last_observation_id is
        'Reference to the temporally last data/observation in the observation table that belongs to this dataset.';

    comment on column dataset.decimals is
        'Number of decimals that should be present in the data/observation values';

    create table dataset_parameter (
       fk_dataset_id int8 not null,
        fk_parameter_id int8 not null,
        primary key (fk_dataset_id, fk_parameter_id)
    );

    comment on table dataset_parameter is
        'Storage of relations between dataset and related parameter';

    comment on column dataset_parameter.fk_dataset_id is
        'The reference to the dataset in the dataset table';

    comment on column dataset_parameter.fk_parameter_id is
        'The reference to the parameter in the dataset parameter';

    create table dataset_reference (
       fk_dataset_id_from int8 not null,
        sort_order int4 not null,
        fk_dataset_id_to int8 not null,
        primary key (fk_dataset_id_from, sort_order)
    );

    comment on table dataset_reference is
        'Storage of referenced datasets, e.g. level zero, medium water level,etc. for water level ';

    comment on column dataset_reference.fk_dataset_id_from is
        'Reference to the dataset that has referenced datasets';

    comment on column dataset_reference.sort_order is
        'Provides the sort order for the referenced datasets.';

    comment on column dataset_reference.fk_dataset_id_to is
        'Reference to the dataset that is the reference';

    create table feature (
       feature_id int8 not null,
        discriminator varchar(255),
        fk_format_id int8 not null,
        identifier varchar(255),
        fk_identifier_codespace_id int8,
        name varchar(255),
        fk_name_codespace_id int8,
        description varchar(255),
        xml text,
        url varchar(255),
        geom GEOMETRY,
        primary key (feature_id)
    );

    comment on table feature is
        'Storage of the features (OfInterest).';

    comment on column feature.feature_id is
        'PK column of the table';

    comment on column feature.discriminator is
        'Indicator used by Hibernate to map value specific entities.';

    comment on column feature.fk_format_id is
        'Reference to the featureType in the format table. Required by the SOS to identify the typ of the feature, e.g. http://www.opengis.net/def/samplingFeatureType/OGC-OM/2.0/SF_SamplingPoint.';

    comment on column feature.identifier is
        'Unique identifier of the feature which can be for filtering, e.g. in the SOS.';

    comment on column feature.fk_identifier_codespace_id is
        'The codespace of the feature identifier, reference to the codespace table.';

    comment on column feature.name is
        'The human readable name of the feature.';

    comment on column feature.fk_name_codespace_id is
        'The codespace of the feature name, reference to the codespace table.';

    comment on column feature.description is
        'A short description of the feature';

    comment on column feature.xml is
        'The XML encoded representation of the feature.';

    comment on column feature.url is
        'Optional URL to an external resource that describes the feature, e.g. a WFS';

    comment on column feature.geom is
        'The geometry/location of feature';

    create table feature_hierarchy (
       fk_child_feature_id int8 not null,
        fk_parent_feature_id int8 not null,
        primary key (fk_parent_feature_id, fk_child_feature_id)
    );

    comment on table feature_hierarchy is
        'Storage of hierarchies between features';

    comment on column feature_hierarchy.fk_child_feature_id is
        'Reference to the child feature in feature table.';

    comment on column feature_hierarchy.fk_parent_feature_id is
        'Reference to the parent feature in feature table.';

    create table feature_i18n (
       feature_i18n_id int8 not null,
        fk_feature_id int8 not null,
        locale varchar(255) not null,
        name varchar(255),
        description varchar(255),
        primary key (feature_i18n_id)
    );

    comment on table feature_i18n is
        'Storage for internationalizations of features.';

    comment on column feature_i18n.feature_i18n_id is
        'PK column of the table';

    comment on column feature_i18n.fk_feature_id is
        'Reference to the feature table this internationalization belongs to.';

    comment on column feature_i18n.locale is
        'Locale/language specification for this entry';

    comment on column feature_i18n.name is
        'Locale/language specific name of the feature';

    comment on column feature_i18n.description is
        'Locale/language specific description of the feature';

    create table feature_parameter (
       fk_feature_id int8 not null,
        fk_parameter_id int8 not null,
        primary key (fk_feature_id, fk_parameter_id)
    );

    comment on table feature_parameter is
        'Storage of relations between feature and related parameter';

    comment on column feature_parameter.fk_feature_id is
        'The reference to the feature in the feature table';

    comment on column feature_parameter.fk_parameter_id is
        'The reference to the parameter in the feature parameter';

    create table format (
       format_id int8 not null,
        definition varchar(255) not null,
        primary key (format_id)
    );

    comment on table format is
        'Storage of types (feature, observation) and formats (procedure)., e.g. http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement and http://www.opengis.net/sensorml/2.0';

    comment on column format.format_id is
        'PK column of the table';

    comment on column format.definition is
        'The definition of the format.';

    create table observation (
       observation_id int8 not null,
        value_type varchar(255) not null,
        fk_dataset_id int8 not null,
        sampling_time_start timestamp with time zone not null,
        sampling_time_end timestamp with time zone not null,
        result_time timestamp with time zone not null,
        identifier varchar(255),
        fk_identifier_codespace_id int8,
        name varchar(255),
        fk_name_codespace_id int8,
        description varchar(255),
        is_deleted int2 default 0 not null check (is_deleted in (1,0)),
        valid_time_start timestamp with time zone default NULL,
        valid_time_end timestamp with time zone default NULL,
        is_child int2 default 0 not null check (is_child in (1,0)),
        is_parent int2 default 0 not null check (is_parent in (1,0)),
        sampling_geometry GEOMETRY,
        value_identifier varchar(255),
        value_name varchar(255),
        value_description varchar(255),
        vertical_from numeric(20, 10) default -99999.00 not null,
        vertical_to numeric(20, 10) default -99999.00 not null,
        value_quantity numeric(19, 2),
        value_text varchar(255),
        value_referenced varchar(255),
        value_count int4,
        value_boolean int2,
        value_category varchar(255),
        primary key (observation_id),
        check (value_type in ('quantity', 'count', 'text', 'category', 'boolean', 'profile', 'complex', 'dataarray', 'geometry', 'blob', 'referenced'))
    );

    comment on column observation.observation_id is
        'PK column of the table';

    comment on column observation.value_type is
        'Indicator used by Hibernate to map value specific entities.';

    comment on column observation.fk_dataset_id is
        'Reference to the dataset to which this data/observation belongs.';

    comment on column observation.sampling_time_start is
        'The timestamp when the observation period has started.';

    comment on column observation.sampling_time_end is
        'The timestamp when the measurement period has finished or the observation took place.';

    comment on column observation.result_time is
        'The timestamp when the observation was published.';

    comment on column observation.identifier is
        'Unique identifier of the data/observation which can be for filtering, e.g. GetObservationById in the SOS';

    comment on column observation.fk_identifier_codespace_id is
        'The codespace of the data/observation identifier, reference to the codespace table.';

    comment on column observation.name is
        'The human readable name of the data/observation.';

    comment on column observation.fk_name_codespace_id is
        'The codespace of the data/observation name, reference to the codespace table.';

    comment on column observation.description is
        'A short description of the data/observation';

    comment on column observation.is_deleted is
        'Flag that indicates if this data/observation is deleted';

    comment on column observation.valid_time_start is
        'The timestamp from when the obervation is valid, e.g. forcasting';

    comment on column observation.valid_time_end is
        'The timestamp until when the obervation is valid, e.g. forcasting';

    comment on column observation.is_child is
        'Flag that indicates if this data/observation is a child observation. Required for composite observation like Complex- or ProfileObservation';

    comment on column observation.is_parent is
        'Flag that indicates if this data/observation is a parent observation. Required for composite observation like Complex- or ProfileObservation';

    comment on column observation.sampling_geometry is
        'The geometry that represents the location where the observation was observed, e.g. mobile observations where this geometry is different from the feature geometry.';

    comment on column observation.value_identifier is
        'Identifier of the value. E.g. used in OGC SWE encoded values like SweText';

    comment on column observation.value_name is
        'Identifier of the name. E.g. used in OGC SWE encoded values like SweText';

    comment on column observation.value_description is
        'Identifier of the description. E.g. used in OGC SWE encoded values like SweText';

    comment on column observation.vertical_from is
        'The start level of a vertical observation, e.g. profiles';

    comment on column observation.vertical_to is
        'The end level or the level of a vertical observation, e.g. profiles';

    comment on column observation.value_quantity is
        'The quantity value of an observation (Measruement)';

    comment on column observation.value_text is
        'The textual value of an observation (TextObservation))';

    comment on column observation.value_referenced is
        'The reference value (URI) of an observation (ReferencedObservation)';

    comment on column observation.value_count is
        'The count/integer value of an observation (CountObservation)';

    comment on column observation.value_boolean is
        'The boolean value of an observation (Boolean/TruthObservation)';

    comment on column observation.value_category is
        'The categorical value of an observation (CategoryObervation)';

    create table observation_i18n (
       data_i18n_id int8 not null,
        fk_data_id int8 not null,
        locale varchar(255) not null,
        name varchar(255),
        description varchar(255),
        primary key (data_i18n_id)
    );

    comment on column observation_i18n.data_i18n_id is
        'PK column of the table';

    comment on column observation_i18n.fk_data_id is
        'Reference to the data table this internationalization belongs to.';

    comment on column observation_i18n.locale is
        'Locale/language specification for this entry';

    comment on column observation_i18n.name is
        'Locale/language specific name of the data entity';

    comment on column observation_i18n.description is
        'Locale/language specific description of the data entity';

    create table observation_parameter (
       fk_observation_id int8 not null,
        fk_parameter_id int8 not null
    );

    comment on table observation_parameter is
        'Storage of relations between data/observation and related parameter';

    comment on column observation_parameter.fk_observation_id is
        'The reference to the data/observation in the observation table';

    comment on column observation_parameter.fk_parameter_id is
        'The reference to the parameter in the data/observation parameter';

    create table offering (
       offering_id int8 not null,
        identifier varchar(255) not null,
        fk_identifier_codespace_id int8,
        name varchar(255),
        fk_name_codespace_id int8,
        description varchar(255),
        sampling_time_start timestamp with time zone,
        sampling_time_end timestamp with time zone,
        result_time_start timestamp with time zone,
        result_time_end timestamp with time zone,
        valid_time_start timestamp with time zone,
        valid_time_end timestamp with time zone,
        geom GEOMETRY,
        primary key (offering_id)
    );

    comment on table offering is
        'Storage of the offerings.';

    comment on column offering.offering_id is
        'PK column of the table';

    comment on column offering.identifier is
        'Unique identifier of the offering which can be for filtering, e.g. in the SOS.';

    comment on column offering.fk_identifier_codespace_id is
        'The codespace of the offering identifier, reference to the codespace table.';

    comment on column offering.name is
        'The human readable name of the offering.';

    comment on column offering.fk_name_codespace_id is
        'The codespace of the offering name, reference to the codespace table.';

    comment on column offering.description is
        'A short description of the offering';

    comment on column offering.sampling_time_start is
        'The minimum samplingTimeStart of all observation that belong to this offering';

    comment on column offering.sampling_time_end is
        'The maximum samplingTimeStart of all observation that belong to this offering';

    comment on column offering.result_time_start is
        'The minimum resultTimeStart of all observation that belong to this offering';

    comment on column offering.result_time_end is
        'The maximum resultTimeEnd of all observation that belong to this offering';

    comment on column offering.valid_time_start is
        'The minimum validTimeStart of all observation that belong to this offering';

    comment on column offering.valid_time_end is
        'The maximum validTimeEnd of all observation that belong to this offering';

    comment on column offering.geom is
        'The envelope/geometry of all features or samplingGeometries of observations that belong to this offering';

    create table offering_feature_type (
       fk_offering_id int8 not null,
        fk_format_id int8 not null,
        primary key (fk_offering_id, fk_format_id)
    );

    comment on table offering_feature_type is
        'Relation to store the valid  featureTypes for the offering';

    comment on column offering_feature_type.fk_offering_id is
        'The related offering';

    comment on column offering_feature_type.fk_format_id is
        'The reference of the related featureType from the format table';

    create table offering_hierarchy (
       fk_child_offering_id int8 not null,
        fk_parent_offering_id int8 not null,
        primary key (fk_parent_offering_id, fk_child_offering_id)
    );

    comment on column offering_hierarchy.fk_child_offering_id is
        'Reference to the child offering in offering table.';

    comment on column offering_hierarchy.fk_parent_offering_id is
        'Reference to the parent offering in offering table.';

    create table offering_i18n (
       offering_i18n_id int8 not null,
        fk_offering_id int8 not null,
        locale varchar(255) not null,
        name varchar(255),
        description varchar(255),
        primary key (offering_i18n_id)
    );

    comment on table offering_i18n is
        'Storage for internationalizations of offerings.';

    comment on column offering_i18n.offering_i18n_id is
        'PK column of the table';

    comment on column offering_i18n.fk_offering_id is
        'Reference to the offering table this internationalization belongs to.';

    comment on column offering_i18n.locale is
        'Locale/language specification for this entry';

    comment on column offering_i18n.name is
        'Locale/language specific name of the offering';

    comment on column offering_i18n.description is
        'Locale/language specific description of the offering';

    create table offering_observation_type (
       fk_offering_id int8 not null,
        fk_format_id int8 not null,
        primary key (fk_offering_id, fk_format_id)
    );

    comment on table offering_observation_type is
        'Relation to store the valid observationTypes for the offering';

    comment on column offering_observation_type.fk_offering_id is
        'The related offering';

    comment on column offering_observation_type.fk_format_id is
        'The reference of the related observationType from the format table';

    create table offering_related_feature (
       fk_offering_id int8 not null,
        fk_related_feature_id int8 not null,
        primary key (fk_offering_id, fk_related_feature_id)
    );

    comment on table offering_related_feature is
        'Storage tfor the relation between offering and relatedFeature';

    comment on column offering_related_feature.fk_offering_id is
        'The related offering';

    comment on column offering_related_feature.fk_related_feature_id is
        'The reference to the relatedFeature from the relatedFeature table';

    create table parameter (
       parameter_id int8 not null,
        type varchar(255) not null,
        name varchar(255) not null,
        last_update timestamp with time zone,
        domain varchar(255),
        value_boolean int2,
        value_category varchar(255),
        fk_unit_id int8,
        value_count int4,
        value_quantity numeric(19, 2),
        value_text varchar(255),
        value_xml text,
        value_json text,
        primary key (parameter_id),
        check (type in ('boolean', 'category', 'count', 'quantity', 'text', 'xml', 'json'))
    );

    comment on table parameter is
        'Storage for additional information for features, observations or datasets';

    comment on column parameter.parameter_id is
        'PK column of the table';

    comment on column parameter.type is
        'Indicator used by Hibernate to map value specific entities.';

    comment on column parameter.name is
        'The name of the parameter';

    comment on column parameter.last_update is
        'Timestamp that provides the time of the last modification of this entry';

    comment on column parameter.domain is
        'The domain this parameter belongs to.';

    comment on column parameter.value_boolean is
        'Storage of a boolean parameter value.';

    comment on column parameter.value_category is
        'Storage of a categorical parameter value.';

    comment on column parameter.fk_unit_id is
        'Reference to the unit of this value in the unit table';

    comment on column parameter.value_count is
        'Storage of a count parameter value.';

    comment on column parameter.value_quantity is
        'Storage of a quantity parameter value.';

    comment on column parameter.value_text is
        'Storage of a textual parameter value.';

    comment on column parameter.value_xml is
        'Storage of a XML encoded parameter value.';

    comment on column parameter.value_json is
        'Storage of a JSON encoded parameter value.';

    create table phenomenon (
       phenomenon_id int8 not null,
        identifier varchar(255) not null,
        fk_identifier_codespace_id int8,
        name varchar(255),
        fk_name_codespace_id int8,
        description varchar(255),
        primary key (phenomenon_id)
    );

    comment on table phenomenon is
        'Storage of the phenomenon/observableProperties';

    comment on column phenomenon.phenomenon_id is
        'PK column of the table';

    comment on column phenomenon.identifier is
        'Unique identifier of the phenomenon which can be for filtering, e.g. in the SOS.';

    comment on column phenomenon.fk_identifier_codespace_id is
        'The codespace of the phenomenon identifier, reference to the codespace table.';

    comment on column phenomenon.name is
        'The human readable name of the phenomenon.';

    comment on column phenomenon.fk_name_codespace_id is
        'The codespace of the phenomenon name, reference to the codespace table.';

    comment on column phenomenon.description is
        'A short description of the phenomenon';

    create table phenomenon_i18n (
       phenomenon_i18n_id int8 not null,
        fk_phenomenon_id int8 not null,
        locale varchar(255) not null,
        name varchar(255),
        description varchar(255),
        primary key (phenomenon_i18n_id)
    );

    comment on table phenomenon_i18n is
        'Storage for internationalizations of phenomenon.';

    comment on column phenomenon_i18n.phenomenon_i18n_id is
        'PK column of the table';

    comment on column phenomenon_i18n.fk_phenomenon_id is
        'Reference to the phenomenon table this internationalization belongs to.';

    comment on column phenomenon_i18n.locale is
        'Locale/language specification for this entry';

    comment on column phenomenon_i18n.name is
        'Locale/language specific name of the phenomenon';

    comment on column phenomenon_i18n.description is
        'Locale/language specific description of the phenomenon';

    create table "procedure" (
       procedure_id int8 not null,
        identifier varchar(255) not null,
        fk_identifier_codespace_id int8,
        name varchar(255),
        fk_name_codespace_id int8,
        description varchar(255),
        is_deleted int2 default 0 not null check (is_deleted in (1,0)),
        description_file text,
        is_reference int2 default 0 check (is_reference in (1,0)),
        fk_type_of_procedure_id int8,
        is_aggregation int2 default 1 check (is_aggregation in (1,0)),
        fk_format_id int8 not null,
        primary key (procedure_id)
    );

    create table procedure_hierarchy (
       fk_child_procedure_id int8 not null,
        fk_parent_procedure_id int8 not null,
        primary key (fk_parent_procedure_id, fk_child_procedure_id)
    );

    create table procedure_history (
       procedure_history_id int8 not null,
        fk_procedure_id int8 not null,
        fk_format_id int8 not null,
        valid_from timestamp with time zone not null,
        valid_to timestamp with time zone default NULL,
        xml text not null,
        primary key (procedure_history_id)
    );

    comment on table procedure_history is
        'Storage of historical procedure descriptions as XML encoded text with period of validity.';

    comment on column procedure_history.procedure_history_id is
        'PK column of the table';

    comment on column procedure_history.fk_procedure_id is
        'Reference to the procedure this entry belongs to.';

    comment on column procedure_history.fk_format_id is
        'Reference to the format of the procedure description, e.g. SensorML 2.0';

    comment on column procedure_history.valid_from is
        'The timestamp from which this procedure description is valid.';

    comment on column procedure_history.valid_to is
        'The timestamp until this procedure description is valid. If null, this procedure description is currently valid';

    comment on column procedure_history.xml is
        'XML representation of this procedure description';

    create table procedure_i18n (
       procedure_i18n_id int8 not null,
        fk_procedure_id int8 not null,
        locale varchar(255) not null,
        name varchar(255),
        description varchar(255),
        short_name varchar(255),
        long_name varchar(255),
        primary key (procedure_i18n_id)
    );

    create table related_dataset (
       fk_dataset_id int8 not null,
        fk_related_dataset_id int8 not null,
        role varchar(255),
        url varchar(255),
        primary key (fk_dataset_id, fk_related_dataset_id)
    );

    comment on table related_dataset is
        'Store the relation of two datasets, e.g. one dataset depends on other datasets to provide context';

    comment on column related_dataset.fk_dataset_id is
        'The reference to the dataset that has a related dataset';

    comment on column related_dataset.fk_related_dataset_id is
        'The reference to the related dataset';

    comment on column related_dataset.role is
        'Definition of the role of the relation';

    comment on column related_dataset.url is
        'URL that point to external information';

    create table related_feature (
       related_feature_id int8 not null,
        fk_feature_id int8 not null,
        role varchar(255) not null,
        primary key (related_feature_id)
    );

    comment on table related_feature is
        'Storage of relations between offerings and features. This table is used by the SOS to fulfill the standard.';

    comment on column related_feature.related_feature_id is
        'PK column of the table';

    comment on column related_feature.fk_feature_id is
        'Reference to the feature that is related to the offering.';

    comment on column related_feature.role is
        'The role of the related feature.';

    create table related_observation (
       fk_observation_id int8 not null,
        fk_related_observation_id int8 not null,
        role varchar(255),
        url varchar(255),
        primary key (fk_observation_id, fk_related_observation_id)
    );

    comment on table related_observation is
        'Store the relation of two observation, e.g. one observation depends on other observations to provide context';

    comment on column related_observation.fk_observation_id is
        'The reference to the dataset that has a related data/observation';

    comment on column related_observation.fk_related_observation_id is
        'The reference to the related data/observation';

    comment on column related_observation.role is
        'Definition of the role of the relation';

    comment on column related_observation.url is
        'URL that point to external information';

    create table result_template (
       result_template_id int8 not null,
        fk_offering_id int8 not null,
        fk_phenomenon_id int8 not null,
        fk_procedure_id int8,
        fk_feature_id int8,
        identifier varchar(255) not null,
        structure text not null,
        encoding text not null,
        primary key (result_template_id)
    );

    comment on table result_template is
        'Storage of templates for the result handling operations';

    comment on column result_template.result_template_id is
        'PK column of the table';

    comment on column result_template.fk_offering_id is
        'The offering that is associated with the result template';

    comment on column result_template.fk_phenomenon_id is
        'The phenomenon that is associated with the result template';

    comment on column result_template.fk_procedure_id is
        'The procedure that is associated with the result template. Can be null if the feature is defined in the structure.';

    comment on column result_template.fk_feature_id is
        'The feature that is associated with the result template. Can be null if the feature is defined in the structure.';

    comment on column result_template.identifier is
        'Unique identifier of the result template used for insertion operation';

    comment on column result_template.structure is
        'The structure of the result template, should be a XML encoded swe:DataRecord';

    comment on column result_template.encoding is
        'The encding of the result template, should be a XML encoded swe:TextEncoding';

    create table service (
       service_id int8 not null,
        name varchar(255) not null,
        description varchar(255),
        url varchar(255),
        type varchar(255),
        version varchar(255),
        primary key (service_id)
    );

    create table unit (
       unit_id int8 not null,
        symbol varchar(255) not null,
        name varchar(255),
        link varchar(255),
        primary key (unit_id)
    );

    comment on table unit is
        'Storage of the units of measurement';

    comment on column unit.unit_id is
        'PK column of the table';

    comment on column unit.symbol is
        'The symbol of the unit, e.g. °C or m.';

    comment on column unit.name is
        'Human readable name of the unit.';

    comment on column unit.link is
        'Reference to a description of the unit.';

    create table unit_i18n (
       unit_i18n_id int8 not null,
        fk_unit_id int8 not null,
        locale varchar(255) not null,
        name varchar(255),
        primary key (unit_i18n_id)
    );

    comment on table unit_i18n is
        'Storage for internationalizations of units.';

    comment on column unit_i18n.unit_i18n_id is
        'PK column of the table';

    comment on column unit_i18n.fk_unit_id is
        'Reference to the unit table this internationalization belongs to.';

    comment on column unit_i18n.locale is
        'Locale/language specification for this unit';

    comment on column unit_i18n.name is
        'Locale/language specific name of the unit';

    create table value_blob (
       fk_observation_id int8 not null,
        value oid,
        primary key (fk_observation_id)
    );

    comment on column value_blob.fk_observation_id is
        'Reference to the data/observation in the observation table';

    comment on column value_blob.value is
        'The blob value of an observation';

    create table value_data_array (
       fk_observation_id int8 not null,
        structure text not null,
        encoding text not null,
        primary key (fk_observation_id)
    );

    comment on column value_data_array.fk_observation_id is
        'Reference to the data/observation';

    comment on column value_data_array.structure is
        'The structure of the data array';

    comment on column value_data_array.encoding is
        'The encoding of the data array values';

    create table value_profile (
       fk_observation_id int8 not null,
        vertical_from_name varchar(255),
        vertical_to_name varchar(255),
        fk_vertical_unit_id int8 not null,
        primary key (fk_observation_id)
    );

    comment on column value_profile.fk_observation_id is
        'Reference to the data/observation';

    comment on column value_profile.vertical_from_name is
        'The name of the vertical from values, e.g. from or depthFrom';

    comment on column value_profile.vertical_to_name is
        'The name of the vertical from values, e.g. to or depthTo';

    comment on column value_profile.fk_vertical_unit_id is
        'The unit of the vertical value, e.g. m';
create index idx_category_identifier on category (identifier);

    alter table if exists codespace 
       add constraint un_codespace_codespace unique (name);
create index idx_dataset_identifier on dataset (identifier);

    alter table if exists dataset 
       add constraint un_dataset_identity unique (fk_procedure_id, fk_phenomenon_id, fk_offering_id, fk_category_id, fk_feature_id);

    alter table if exists dataset 
       add constraint un_dataset_identifier unique (identifier);
create index idx_feature_identifier on feature (identifier);

    alter table if exists feature 
       add constraint un_feature_identifier unique (identifier);

    alter table if exists feature 
       add constraint un_feature_url unique (url);

    alter table if exists format 
       add constraint un_format_definition unique (definition);
create index idx_sampling_time_start on observation (sampling_time_start);
create index idx_sampling_time_end on observation (sampling_time_end);
create index idx_result_time on observation (result_time);

    alter table if exists observation 
       add constraint un_data_identity unique (fk_dataset_id, sampling_time_start, sampling_time_end, result_time, vertical_from, vertical_to);

    alter table if exists observation 
       add constraint un_data_identifier unique (identifier);
create index idx_offering_identifier on offering (identifier);

    alter table if exists offering 
       add constraint un_offering_identifier unique (identifier);
create index idx_param_name on parameter (name);
create index idx_phenomenon_identifier on phenomenon (identifier);

    alter table if exists phenomenon 
       add constraint fk_phenomenon_id unique (identifier);
create index idx_procedure_identifier on "procedure" (identifier);

    alter table if exists "procedure" 
       add constraint un_procedure_identifier unique (identifier);
create index idx_start_time on procedure_history (valid_from);
create index idx_end_time on procedure_history (valid_to);
create index idx_related_observation on related_observation (fk_observation_id);
create index idx_result_template_offering on result_template (fk_offering_id);
create index idx_result_template_phenomenon on result_template (fk_phenomenon_id);
create index idx_result_template_procedure on result_template (fk_procedure_id);
create index idx_result_template_feature on result_template (fk_feature_id);
create index idx_result_template_identifier on result_template (identifier);

    alter table if exists service 
       add constraint un_service_name unique (name);

    alter table if exists unit 
       add constraint un_unit_symbol unique (symbol);

    alter table if exists category_i18n 
       add constraint fk_category 
       foreign key (fk_category_id) 
       references category;

    alter table if exists composite_observation 
       add constraint fk_composite_observation_child 
       foreign key (fk_child_observation_id) 
       references observation;

    alter table if exists composite_observation 
       add constraint fk_composite_observation 
       foreign key (fk_parent_observation_id) 
       references observation;

    alter table if exists composite_phenomenon 
       add constraint fk_phenomenon_parent 
       foreign key (fk_parent_phenomenon_id) 
       references phenomenon;

    alter table if exists composite_phenomenon 
       add constraint fk_phenomenon_child 
       foreign key (fk_child_phenomenon_id) 
       references phenomenon;

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
       add constraint fk_dataset_observation_type 
       foreign key (fk_format_id) 
       references format;

    alter table if exists dataset 
       add constraint fk_dataset_identifier_codespace 
       foreign key (fk_identifier_codespace_id) 
       references codespace;

    alter table if exists dataset 
       add constraint fk_dataset_name_codespace 
       foreign key (fk_name_codespace_id) 
       references codespace;

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

    alter table if exists dataset_parameter 
       add constraint fk_parameter_dataset 
       foreign key (fk_parameter_id) 
       references parameter;

    alter table if exists dataset_parameter 
       add constraint fk_dataset_parameter 
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

    alter table if exists feature 
       add constraint fk_feature_format 
       foreign key (fk_format_id) 
       references format;

    alter table if exists feature 
       add constraint fk_feature_identifier_codespace 
       foreign key (fk_identifier_codespace_id) 
       references codespace;

    alter table if exists feature 
       add constraint fk_feature_name_codespace 
       foreign key (fk_name_codespace_id) 
       references codespace;

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

    alter table if exists feature_parameter 
       add constraint fk_parameter_feature 
       foreign key (fk_parameter_id) 
       references parameter;

    alter table if exists feature_parameter 
       add constraint fk_feature_parameter 
       foreign key (fk_feature_id) 
       references feature;

    alter table if exists observation 
       add constraint fk_dataset 
       foreign key (fk_dataset_id) 
       references dataset;

    alter table if exists observation 
       add constraint fk_data_identifier_codespace 
       foreign key (fk_identifier_codespace_id) 
       references codespace;

    alter table if exists observation 
       add constraint fk_data_name_codespace 
       foreign key (fk_name_codespace_id) 
       references codespace;

    alter table if exists observation_i18n 
       add constraint fk_data 
       foreign key (fk_data_id) 
       references observation;

    alter table if exists observation_parameter 
       add constraint fk_parameter_observation 
       foreign key (fk_parameter_id) 
       references parameter;

    alter table if exists observation_parameter 
       add constraint fk_observation_parameter 
       foreign key (fk_observation_id) 
       references observation;

    alter table if exists offering 
       add constraint fk_offering_identifier_codespace 
       foreign key (fk_identifier_codespace_id) 
       references codespace;

    alter table if exists offering 
       add constraint fk_offering_name_codespace 
       foreign key (fk_name_codespace_id) 
       references codespace;

    alter table if exists offering_feature_type 
       add constraint fk_feature_type_offering 
       foreign key (fk_format_id) 
       references format;

    alter table if exists offering_feature_type 
       add constraint fk_offering_feature_type 
       foreign key (fk_offering_id) 
       references offering;

    alter table if exists offering_hierarchy 
       add constraint fk_offering_parent 
       foreign key (fk_parent_offering_id) 
       references offering;

    alter table if exists offering_hierarchy 
       add constraint fk_offering_child 
       foreign key (fk_child_offering_id) 
       references offering;

    alter table if exists offering_i18n 
       add constraint fk_offering 
       foreign key (fk_offering_id) 
       references offering;

    alter table if exists offering_observation_type 
       add constraint fk_observation_type_offering 
       foreign key (fk_format_id) 
       references format;

    alter table if exists offering_observation_type 
       add constraint fk_offering_observation_type 
       foreign key (fk_offering_id) 
       references offering;

    alter table if exists offering_related_feature 
       add constraint fk_offering_related_feature 
       foreign key (fk_related_feature_id) 
       references related_feature;

    alter table if exists offering_related_feature 
       add constraint fk_related_feature_offering 
       foreign key (fk_offering_id) 
       references offering;

    alter table if exists parameter 
       add constraint fk_param_unit 
       foreign key (fk_unit_id) 
       references unit;

    alter table if exists phenomenon 
       add constraint fk_phenomenon_identifier_codespace 
       foreign key (fk_identifier_codespace_id) 
       references codespace;

    alter table if exists phenomenon 
       add constraint fk_phenomenon_name_codespace 
       foreign key (fk_name_codespace_id) 
       references codespace;

    alter table if exists phenomenon_i18n 
       add constraint fk_phenomenon 
       foreign key (fk_phenomenon_id) 
       references phenomenon;

    alter table if exists "procedure" 
       add constraint fk_procedure_identifier_codespace 
       foreign key (fk_identifier_codespace_id) 
       references codespace;

    alter table if exists "procedure" 
       add constraint fk_procedure_name_codespace 
       foreign key (fk_name_codespace_id) 
       references codespace;

    alter table if exists "procedure" 
       add constraint fk_type_of 
       foreign key (fk_type_of_procedure_id) 
       references "procedure";

    alter table if exists "procedure" 
       add constraint fk_procedure_format 
       foreign key (fk_format_id) 
       references format;

    alter table if exists procedure_hierarchy 
       add constraint fk_procedure_parent 
       foreign key (fk_parent_procedure_id) 
       references "procedure";

    alter table if exists procedure_hierarchy 
       add constraint fk_procedure_child 
       foreign key (fk_child_procedure_id) 
       references "procedure";

    alter table if exists procedure_history 
       add constraint fk_procedure_history 
       foreign key (fk_procedure_id) 
       references "procedure";

    alter table if exists procedure_history 
       add constraint fk_pdf_id 
       foreign key (fk_format_id) 
       references format;

    alter table if exists procedure_i18n 
       add constraint fk_procedure 
       foreign key (fk_procedure_id) 
       references "procedure";

    alter table if exists related_dataset 
       add constraint fk_rel_dataset_dataset 
       foreign key (fk_dataset_id) 
       references dataset;

    alter table if exists related_dataset 
       add constraint fk_rel_dataset_rel_dataset 
       foreign key (fk_related_dataset_id) 
       references dataset;

    alter table if exists related_feature 
       add constraint fk_related_feature 
       foreign key (fk_feature_id) 
       references feature;

    alter table if exists related_observation 
       add constraint fk_related_observation 
       foreign key (fk_observation_id) 
       references observation;

    alter table if exists related_observation 
       add constraint fk_rel_obs_related 
       foreign key (fk_related_observation_id) 
       references observation;

    alter table if exists result_template 
       add constraint fk_result_template_offering 
       foreign key (fk_offering_id) 
       references offering;

    alter table if exists result_template 
       add constraint fk_result_template_phenomenon 
       foreign key (fk_phenomenon_id) 
       references phenomenon;

    alter table if exists result_template 
       add constraint fk_result_template_procedure 
       foreign key (fk_procedure_id) 
       references "procedure";

    alter table if exists result_template 
       add constraint fk_result_template_feature 
       foreign key (fk_feature_id) 
       references feature;

    alter table if exists unit_i18n 
       add constraint fk_unit 
       foreign key (fk_unit_id) 
       references unit;

    alter table if exists value_blob 
       add constraint fk_blob_value 
       foreign key (fk_observation_id) 
       references observation;

    alter table if exists value_data_array 
       add constraint fk_data_array_value 
       foreign key (fk_observation_id) 
       references observation;

    alter table if exists value_profile 
       add constraint fk_profile_value 
       foreign key (fk_observation_id) 
       references observation;

    alter table if exists value_profile 
       add constraint fk_profile_unit 
       foreign key (fk_vertical_unit_id) 
       references unit;
