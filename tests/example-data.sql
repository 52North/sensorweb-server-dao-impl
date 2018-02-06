--
-- PostgreSQL database dump
--

-- Dumped from database version 9.5.7
-- Dumped by pg_dump version 9.5.7

-- Started on 2017-07-18 15:11:03 CEST

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

SET search_path = public, pg_catalog;


COPY codespace (codespace_id, name) FROM stdin;
1	http://www.opengis.net/def/nil/OGC/0/unknown
\.



COPY format (format_id, definition) FROM stdin;
1	http://www.opengis.net/def/samplingFeatureType/OGC-OM/2.0/SF_SamplingPoint
2	http://www.opengis.net/def/nil/OGC/0/unknown
3	http://www.opengis.net/sensorML/1.0.1
4	http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement
5	http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_CountObservation
6	http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_TruthObservation
7	http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_CategoryObservation
8	http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_TextObservation
\.





COPY feature (feature_id, discriminator, fk_format_id, identifier, fk_identifier_codespace_id, name, fk_name_codespace_id, description, geom, xml, url) FROM stdin;
1	\N	2	http://www.52north.org/test/featureOfInterest/world	1	\N	1	\N	\N	\N	\N
2	\N	2	http://www.52north.org/test/featureOfInterest/1	1	con terra	1	\N	0101000020E61000003F726BD26DE91E407D5EF1D423F14940	\N	\N
3	\N	2	http://www.52north.org/test/featureOfInterest/2	1	ESRI	1	\N	0101000020E6100000EB1D6E87864C5DC08255F5F23B074140	\N	\N
4	\N	2	http://www.52north.org/test/featureOfInterest/3	1	Kisters	1	\N	0101000020E610000014AAB2C82E8718400576C70892644940	\N	\N
5	\N	2	http://www.52north.org/test/featureOfInterest/4	1	IfGI	1	\N	0101000020E61000003F726BD26DE91E407D5EF1D423F14940	\N	\N
6	\N	2	http://www.52north.org/test/featureOfInterest/5	1	TU-Dresden	1	\N	0101000020E6100000404EB4AB90722B401DE6CB0BB0834940	\N	\N
7	\N	2	http://www.52north.org/test/featureOfInterest/6	1	Hochschule Bochum	1	\N	0101000020E6100000083E062B4E151D4090D959F44EB94940	\N	\N
8	\N	2	http://www.52north.org/test/featureOfInterest/7	1	ITC	1	\N	0101000020E610000000958FEE31221140E45F15B9F1054A40	\N	\N
9	\N	2	http://www.52north.org/test/featureOfInterest/8	1	DLZ-IT	1	\N	\N	\N	\N
10	\N	2	http://www.52north.org/test/featureOfInterest/Heiden	1	Heiden	1	\N	0101000020E61000008C118942CBBA1B404D874ECFBBE94940	\N	\N
11	\N	2	http://www.52north.org/test/featureOfInterest/Münster/FE101	1	Münster/FE101	1	\N	0101000020E610000099B9C0E5B1861E405473B9C150F94940	\N	\N
12	\N	2	http://www.52north.org/test/featureOfInterest/Portland	1	Portland	1	\N	0101000020E61000001DC9E53FA4AB5EC0C3F5285C8FC24640	\N	\N
13	\N	2	http://www.52north.org/test/featureOfInterest/TODO	1	TODO	1	\N	0101000020E610000000000000000000000000000000000000	\N	\N
\.



COPY phenomenon (phenomenon_id, identifier, fk_identifier_codespace_id, name, fk_name_codespace_id, description, is_hidden_child) FROM stdin;
1	http://www.52north.org/test/observableProperty/1	1	test_observable_property_1	1	\N	0
2	http://www.52north.org/test/observableProperty/2	1	test_observable_property_2	1	\N	0
3	http://www.52north.org/test/observableProperty/3	1	test_observable_property_3	1	\N	0
4	http://www.52north.org/test/observableProperty/4	1	test_observable_property_4	1	\N	0
5	http://www.52north.org/test/observableProperty/5	1	test_observable_property_5	1	\N	0
6	http://www.52north.org/test/observableProperty/6	1	test_observable_property_6	1	\N	0
7	http://www.52north.org/test/observableProperty/7	1	test_observable_property_7	1	\N	0
8	http://www.52north.org/test/observableProperty/8	1	test_observable_property_8	1	\N	0
9	http://www.52north.org/test/observableProperty/developer	1	developer	1	\N	0
\.



COPY category (category_id, identifier, name, description) FROM stdin;
1	http://www.52north.org/test/observableProperty/1	test_observable_property_1	\N
2	http://www.52north.org/test/observableProperty/2	test_observable_property_2	\N
3	http://www.52north.org/test/observableProperty/3	test_observable_property_3	\N
4	http://www.52north.org/test/observableProperty/4	test_observable_property_4	\N
5	http://www.52north.org/test/observableProperty/5	test_observable_property_5	\N
6	http://www.52north.org/test/observableProperty/6	test_observable_property_6	\N
7	http://www.52north.org/test/observableProperty/7	test_observable_property_7	\N
8	http://www.52north.org/test/observableProperty/8	test_observable_property_8	\N
9	http://www.52north.org/test/observableProperty/developer	developer	\N
\.




COPY offering (offering_id, identifier, fk_identifier_codespace_id, name, fk_name_codespace_id, description, sampling_time_start, sampling_time_end, result_time_start, result_time_end) FROM stdin;
1	http://www.52north.org/test/offering/1	\N	Offering for sensor 1	\N	\N	1970-01-01 00:00:00	1970-01-01 00:00:00	1970-01-01 00:00:00	1970-01-01 00:00:00
2	http://www.52north.org/test/offering/2	\N	Offering for sensor 2	\N	\N	1970-01-01 00:00:00	1970-01-01 00:00:00	1970-01-01 00:00:00	1970-01-01 00:00:00
3	http://www.52north.org/test/offering/3	\N	Offering for sensor 3	\N	\N	1970-01-01 00:00:00	1970-01-01 00:00:00	1970-01-01 00:00:00	1970-01-01 00:00:00
4	http://www.52north.org/test/offering/4	\N	Offering for sensor 2	\N	\N	1970-01-01 00:00:00	1970-01-01 00:00:00	1970-01-01 00:00:00	1970-01-01 00:00:00
5	http://www.52north.org/test/offering/5	\N	Offering for sensor 5	\N	\N	1970-01-01 00:00:00	1970-01-01 00:00:00	1970-01-01 00:00:00	1970-01-01 00:00:00
6	http://www.52north.org/test/offering/6	\N	Offering for sensor 6	\N	\N	1970-01-01 00:00:00	1970-01-01 00:00:00	1970-01-01 00:00:00	1970-01-01 00:00:00
7	http://www.52north.org/test/offering/7	\N	Offering for sensor 7	\N	\N	1970-01-01 00:00:00	1970-01-01 00:00:00	1970-01-01 00:00:00	1970-01-01 00:00:00
8	http://www.52north.org/test/offering/8	\N	Offering for sensor 8	\N	\N	1970-01-01 00:00:00	1970-01-01 00:00:00	1970-01-01 00:00:00	1970-01-01 00:00:00
9	http://www.52north.org/test/offering/developer	\N	Offering for procedure developer	\N	\N	1970-01-01 00:00:00	1970-01-01 00:00:00	1970-01-01 00:00:00	1970-01-01 00:00:00
\.



COPY procedure (procedure_id, fk_format_id, identifier, fk_identifier_codespace_id, name, fk_name_codespace_id, description, is_deleted, description_file, is_reference, fk_type_of_procedure_id, is_aggregation, is_mobile, is_insitu) FROM stdin;
1	3	http://www.52north.org/test/procedure/1	\N	con terra	\N	\N	0	\N	0	\N	0	0	1
2	3	http://www.52north.org/test/procedure/2	\N	ESRI	\N	\N	0	\N	0	\N	0	0	1
3	3	http://www.52north.org/test/procedure/3	\N	Kisters	\N	\N	0	\N	0	\N	0	0	1
4	3	http://www.52north.org/test/procedure/4	\N	IfGI	\N	\N	0	\N	0	\N	0	0	1
5	3	http://www.52north.org/test/procedure/5	\N	TU-Dresden	\N	\N	1	\N	0	\N	0	0	1
6	3	http://www.52north.org/test/procedure/6	\N	Hochschule Bochum	\N	\N	0	\N	0	\N	0	0	1
7	3	http://www.52north.org/test/procedure/7	\N	ITC	\N	\N	0	\N	0	\N	0	0	1
8	3	http://www.52north.org/test/procedure/8	\N	DLZ-IT	\N	\N	0	\N	0	\N	0	1	1
9	3	http://www.52north.org/test/procedure/developer	\N	http://www.52north.org/test/procedure/developer	\N	\N	0	\N	0	\N	0	0	1
\.



COPY unit (unit_id, symbol, name, link) FROM stdin;
1	test_unit_1	\N	\N
2	test_unit_4	\N	\N
3	test_unit_6	\N	\N
4	test_unit_7	\N	\N
5	test_unit_8	\N	\N
6	m	\N	\N
\.



COPY dataset (dataset_id, fk_feature_id, fk_category_id, fk_phenomenon_id, fk_procedure_id, fk_format_id, fk_offering_id, is_deleted, is_published, is_hidden_child, first_time, last_time, fk_unit_id, identifier, fk_identifier_codespace_id, name, fk_name_codespace_id, description, value_type, decimals) FROM stdin;
1	2	1	1	1	4	1	0	1	0	2012-11-19 13:00:00	2012-11-19 13:09:00	1	\N	\N	\N	\N	\N	quantity-profile	3
2	3	2	2	2	5	2	0	1	0	2012-11-19 13:00:00	2012-11-19 13:09:00	\N	\N	\N	\N	\N	\N	count	\N
3	4	3	3	3	6	3	0	1	0	2012-11-19 13:00:00	2012-11-19 13:09:00	\N	\N	\N	\N	\N	\N	boolean	\N
4	5	4	4	4	7	4	0	1	0	2012-11-19 13:00:00	2012-11-19 13:09:00	\N	\N	\N	\N	\N	\N	category	\N
5	6	5	5	5	8	5	1	1	0	2012-11-19 13:00:00	2012-11-19 13:09:00	\N	\N	\N	\N	\N	\N	text	\N
6	7	6	6	6	4	6	0	1	0	2012-11-19 13:00:00	2012-11-19 13:09:00	3	\N	\N	\N	\N	\N	quantity	3
7	8	7	7	7	4	7	0	1	0	2012-11-19 13:00:00	2012-11-19 13:09:00	4	\N	\N	\N	\N	\N	quantity	3
8	9	8	8	8	4	8	0	1	0	2012-11-19 13:00:00	2012-11-19 13:49:59	5	\N	\N	\N	\N	\N	quantity	3
9	10	9	9	9	8	9	0	1	0	2008-10-29 00:00:00	2008-10-29 00:00:00	\N	\N	\N	\N	\N	\N	text	\N
10	11	9	9	9	8	9	0	0	0	2008-10-29 00:00:00	2008-10-29 00:00:00	\N	\N	\N	\N	\N	\N	text	\N
11	12	9	9	9	8	9	0	1	0	2008-10-29 00:00:00	2008-10-29 00:00:00	\N	\N	\N	\N	\N	\N	text	\N
12	13	9	9	9	8	9	0	1	0	2012-12-31 23:00:00	2012-12-31 23:00:00	\N	\N	\N	\N	\N	\N	text	\N
\.



COPY observation (observation_id, fk_dataset_id, sampling_time_start, sampling_time_end, result_time, identifier, fk_identifier_codespace_id, name, fk_name_codespace_id, description, is_deleted, is_child, is_parent, valid_time_start, valid_time_end, sampling_geometry, value_count, value_boolean, value_category, value_quantity, value_text, value_type, vertical_from, vertical_to) FROM stdin;
1	1	2012-11-19 13:00:00	2012-11-19 13:00:00	2012-11-19 13:00:00	http://www.52north.org/test/profile-observation/1	1	\N	1	\N	0	0	1	\N	\N	\N	\N	\N	\N	\N	\N	profile	10	20
10	1	2012-11-19 13:09:00	2012-11-19 13:09:00	2012-11-19 13:09:00	http://www.52north.org/test/profile-observation/10	1	\N	1	\N	0	0	1	\N	\N	\N	\N	\N	\N	\N	\N	profile	10	20
11	2	2012-11-19 13:00:00	2012-11-19 13:00:00	2012-11-19 13:00:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	0	\N	\N	\N	\N	count	-99999.00	-99999.00
12	2	2012-11-19 13:01:00	2012-11-19 13:01:00	2012-11-19 13:01:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	1	\N	\N	\N	\N	count	-99999.00	-99999.00
13	2	2012-11-19 13:02:00	2012-11-19 13:02:00	2012-11-19 13:02:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	2	\N	\N	\N	\N	count	-99999.00	-99999.00
14	2	2012-11-19 13:03:00	2012-11-19 13:03:00	2012-11-19 13:03:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	3	\N	\N	\N	\N	count	-99999.00	-99999.00
15	2	2012-11-19 13:04:00	2012-11-19 13:04:00	2012-11-19 13:04:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	4	\N	\N	\N	\N	count	-99999.00	-99999.00
16	2	2012-11-19 13:05:00	2012-11-19 13:05:00	2012-11-19 13:05:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	5	\N	\N	\N	\N	count	-99999.00	-99999.00
17	2	2012-11-19 13:06:00	2012-11-19 13:06:00	2012-11-19 13:06:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	6	\N	\N	\N	\N	count	-99999.00	-99999.00
18	2	2012-11-19 13:07:00	2012-11-19 13:07:00	2012-11-19 13:07:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	7	\N	\N	\N	\N	count	-99999.00	-99999.00
19	2	2012-11-19 13:08:00	2012-11-19 13:08:00	2012-11-19 13:08:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	8	\N	\N	\N	\N	count	-99999.00	-99999.00
20	2	2012-11-19 13:09:00	2012-11-19 13:09:00	2012-11-19 13:09:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	9	\N	\N	\N	\N	count	-99999.00	-99999.00
21	3	2012-11-19 13:00:00	2012-11-19 13:00:00	2012-11-19 13:00:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	1	\N	\N	\N	boolean	-99999.00	-99999.00
22	3	2012-11-19 13:01:00	2012-11-19 13:01:00	2012-11-19 13:01:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	1	\N	\N	\N	boolean	-99999.00	-99999.00
23	3	2012-11-19 13:02:00	2012-11-19 13:02:00	2012-11-19 13:02:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	0	\N	\N	\N	boolean	-99999.00	-99999.00
24	3	2012-11-19 13:03:00	2012-11-19 13:03:00	2012-11-19 13:03:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	1	\N	\N	\N	boolean	-99999.00	-99999.00
25	3	2012-11-19 13:04:00	2012-11-19 13:04:00	2012-11-19 13:04:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	0	\N	\N	\N	boolean	-99999.00	-99999.00
26	3	2012-11-19 13:05:00	2012-11-19 13:05:00	2012-11-19 13:05:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	0	\N	\N	\N	boolean	-99999.00	-99999.00
27	3	2012-11-19 13:06:00	2012-11-19 13:06:00	2012-11-19 13:06:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	1	\N	\N	\N	boolean	-99999.00	-99999.00
28	3	2012-11-19 13:07:00	2012-11-19 13:07:00	2012-11-19 13:07:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	1	\N	\N	\N	boolean	-99999.00	-99999.00
29	3	2012-11-19 13:08:00	2012-11-19 13:08:00	2012-11-19 13:08:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	0	\N	\N	\N	boolean	-99999.00	-99999.00
30	3	2012-11-19 13:09:00	2012-11-19 13:09:00	2012-11-19 13:09:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	1	\N	\N	\N	boolean	-99999.00	-99999.00
31	4	2012-11-19 13:00:00	2012-11-19 13:00:00	2012-11-19 13:00:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	test_category_0	\N	\N	category	-99999.00	-99999.00
32	4	2012-11-19 13:01:00	2012-11-19 13:01:00	2012-11-19 13:01:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	test_category_1	\N	\N	category	-99999.00	-99999.00
33	4	2012-11-19 13:02:00	2012-11-19 13:02:00	2012-11-19 13:02:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	test_category_2	\N	\N	category	-99999.00	-99999.00
34	4	2012-11-19 13:03:00	2012-11-19 13:03:00	2012-11-19 13:03:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	test_category_3	\N	\N	category	-99999.00	-99999.00
35	4	2012-11-19 13:04:00	2012-11-19 13:04:00	2012-11-19 13:04:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	test_category_4	\N	\N	category	-99999.00	-99999.00
36	4	2012-11-19 13:05:00	2012-11-19 13:05:00	2012-11-19 13:05:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	test_category_5	\N	\N	category	-99999.00	-99999.00
37	4	2012-11-19 13:06:00	2012-11-19 13:06:00	2012-11-19 13:06:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	test_category_6	\N	\N	category	-99999.00	-99999.00
38	4	2012-11-19 13:07:00	2012-11-19 13:07:00	2012-11-19 13:07:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	test_category_7	\N	\N	category	-99999.00	-99999.00
39	4	2012-11-19 13:08:00	2012-11-19 13:08:00	2012-11-19 13:08:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	test_category_8	\N	\N	category	-99999.00	-99999.00
40	4	2012-11-19 13:09:00	2012-11-19 13:09:00	2012-11-19 13:09:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	test_category_9	\N	\N	category	-99999.00	-99999.00
41	5	2012-11-19 13:00:00	2012-11-19 13:00:00	2012-11-19 13:00:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	\N	\N	test_text_0	text	-99999.00	-99999.00
42	5	2012-11-19 13:01:00	2012-11-19 13:01:00	2012-11-19 13:01:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	\N	\N	test_text_1	text	-99999.00	-99999.00
43	5	2012-11-19 13:02:00	2012-11-19 13:02:00	2012-11-19 13:02:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	\N	\N	test_text_2	text	-99999.00	-99999.00
44	5	2012-11-19 13:03:00	2012-11-19 13:03:00	2012-11-19 13:03:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	\N	\N	test_text_3	text	-99999.00	-99999.00
45	5	2012-11-19 13:04:00	2012-11-19 13:04:00	2012-11-19 13:04:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	\N	\N	test_text_4	text	-99999.00	-99999.00
46	5	2012-11-19 13:05:00	2012-11-19 13:05:00	2012-11-19 13:05:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	\N	\N	test_text_5	text	-99999.00	-99999.00
47	5	2012-11-19 13:06:00	2012-11-19 13:06:00	2012-11-19 13:06:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	\N	\N	test_text_6	text	-99999.00	-99999.00
48	5	2012-11-19 13:07:00	2012-11-19 13:07:00	2012-11-19 13:07:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	\N	\N	test_text_7	text	-99999.00	-99999.00
49	5	2012-11-19 13:08:00	2012-11-19 13:08:00	2012-11-19 13:08:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	\N	\N	test_text_8	text	-99999.00	-99999.00
50	5	2012-11-19 13:09:00	2012-11-19 13:09:00	2012-11-19 13:09:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	\N	\N	test_text_9	text	-99999.00	-99999.00
51	6	2012-11-19 13:00:00	2012-11-19 13:00:00	2012-11-19 13:00:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	\N	2.0	\N	quantity	-99999.00	-99999.00
52	6	2012-11-19 13:01:00	2012-11-19 13:01:00	2012-11-19 13:01:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	\N	2.1	\N	quantity	-99999.00	-99999.00
53	6	2012-11-19 13:02:00	2012-11-19 13:02:00	2012-11-19 13:02:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	\N	2.2	\N	quantity	-99999.00	-99999.00
54	6	2012-11-19 13:03:00	2012-11-19 13:03:00	2012-11-19 13:03:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	\N	2.3	\N	quantity	-99999.00	-99999.00
55	6	2012-11-19 13:04:00	2012-11-19 13:04:00	2012-11-19 13:04:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	\N	2.4	\N	quantity	-99999.00	-99999.00
56	6	2012-11-19 13:05:00	2012-11-19 13:05:00	2012-11-19 13:05:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	\N	2.5	\N	quantity	-99999.00	-99999.00
57	6	2012-11-19 13:06:00	2012-11-19 13:06:00	2012-11-19 13:06:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	\N	2.6	\N	quantity	-99999.00	-99999.00
58	6	2012-11-19 13:07:00	2012-11-19 13:07:00	2012-11-19 13:07:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	\N	2.7	\N	quantity	-99999.00	-99999.00
59	6	2012-11-19 13:08:00	2012-11-19 13:08:00	2012-11-19 13:08:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	\N	2.8	\N	quantity	-99999.00	-99999.00
60	6	2012-11-19 13:09:00	2012-11-19 13:09:00	2012-11-19 13:09:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	\N	2.9	\N	quantity	-99999.00	-99999.00
61	7	2012-11-19 13:00:00	2012-11-19 13:00:00	2012-11-19 13:00:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	\N	3.0	\N	quantity	-99999.00	-99999.00
62	7	2012-11-19 13:01:00	2012-11-19 13:01:00	2012-11-19 13:01:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	\N	3.1	\N	quantity	-99999.00	-99999.00
63	7	2012-11-19 13:02:00	2012-11-19 13:02:00	2012-11-19 13:02:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	\N	3.2	\N	quantity	-99999.00	-99999.00
64	7	2012-11-19 13:03:00	2012-11-19 13:03:00	2012-11-19 13:03:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	\N	3.3	\N	quantity	-99999.00	-99999.00
65	7	2012-11-19 13:04:00	2012-11-19 13:04:00	2012-11-19 13:04:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	\N	3.4	\N	quantity	-99999.00	-99999.00
66	7	2012-11-19 13:05:00	2012-11-19 13:05:00	2012-11-19 13:05:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	\N	3.5	\N	quantity	-99999.00	-99999.00
67	7	2012-11-19 13:06:00	2012-11-19 13:06:00	2012-11-19 13:06:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	\N	3.6	\N	quantity	-99999.00	-99999.00
68	7	2012-11-19 13:07:00	2012-11-19 13:07:00	2012-11-19 13:07:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	\N	3.7	\N	quantity	-99999.00	-99999.00
69	7	2012-11-19 13:08:00	2012-11-19 13:08:00	2012-11-19 13:08:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	\N	3.8	\N	quantity	-99999.00	-99999.00
70	7	2012-11-19 13:09:00	2012-11-19 13:09:00	2012-11-19 13:09:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	\N	3.9	\N	quantity	-99999.00	-99999.00
71	8	2012-11-19 13:00:00	2012-11-19 13:04:59	2012-11-18 13:00:00	http://www.52north.org/test/observation/8_71_resulttime_2012-11-18	1	\N	1	\N	0	0	0	\N	\N	0101000020E61000000000000000001C400000000000004940	\N	\N	\N	4.0	\N	quantity	-99999.00	-99999.00
72	8	2012-11-19 13:05:00	2012-11-19 13:09:59	2012-11-18 13:00:00	http://www.52north.org/test/observation/8_72_resulttime_2012-11-18	1	\N	1	\N	0	0	0	\N	\N	0101000020E6100000CDCCCCCCCCCC1C40CDCCCCCCCC4C4940	\N	\N	\N	4.1	\N	quantity	-99999.00	-99999.00
73	8	2012-11-19 13:10:00	2012-11-19 13:14:59	2012-11-18 13:00:00	http://www.52north.org/test/observation/8_73_resulttime_2012-11-18	1	\N	1	\N	0	0	0	\N	\N	0101000020E61000003333333333331F40CDCCCCCCCCCC4940	\N	\N	\N	4.2	\N	quantity	-99999.00	-99999.00
74	8	2012-11-19 13:15:00	2012-11-19 13:19:59	2012-11-18 13:00:00	http://www.52north.org/test/observation/8_74_resulttime_2012-11-18	1	\N	1	\N	0	0	0	\N	\N	0101000020E6100000CDCCCCCCCCCC20409A99999999994940	\N	\N	\N	4.3	\N	quantity	-99999.00	-99999.00
75	8	2012-11-19 13:20:00	2012-11-19 13:24:59	2012-11-18 13:00:00	http://www.52north.org/test/observation/8_75_resulttime_2012-11-18	1	\N	1	\N	0	0	0	\N	\N	0101000020E61000009A99999999991D409A99999999194940	\N	\N	\N	4.4	\N	quantity	-99999.00	-99999.00
76	8	2012-11-19 13:25:00	2012-11-19 13:29:59	2012-11-18 13:00:00	http://www.52north.org/test/observation/8_76_resulttime_2012-11-18	1	\N	1	\N	0	0	0	\N	\N	0101000020E61000006666666666661E406666666666664940	\N	\N	\N	4.5	\N	quantity	-99999.00	-99999.00
77	8	2012-11-19 13:30:00	2012-11-19 13:34:59	2012-11-18 13:00:00	http://www.52north.org/test/observation/8_77_resulttime_2012-11-18	1	\N	1	\N	0	0	0	\N	\N	0101000020E610000000000000000020406666666666664940	\N	\N	\N	4.6	\N	quantity	-99999.00	-99999.00
78	8	2012-11-19 13:35:00	2012-11-19 13:39:59	2012-11-18 13:00:00	http://www.52north.org/test/observation/8_78_resulttime_2012-11-18	1	\N	1	\N	0	0	0	\N	\N	0101000020E610000000000000000021406666666666664940	\N	\N	\N	4.7	\N	quantity	-99999.00	-99999.00
79	8	2012-11-19 13:40:00	2012-11-19 13:44:59	2012-11-18 13:00:00	http://www.52north.org/test/observation/8_79_resulttime_2012-11-18	1	\N	1	\N	0	0	0	\N	\N	0101000020E610000033333333333320409A99999999194940	\N	\N	\N	4.8	\N	quantity	-99999.00	-99999.00
80	8	2012-11-19 13:45:00	2012-11-19 13:49:59	2012-11-18 13:00:00	http://www.52north.org/test/observation/8_80_resulttime_2012-11-18	1	\N	1	\N	0	0	0	\N	\N	0101000020E610000033333333333321400000000000404940	\N	\N	\N	4.9	\N	quantity	-99999.00	-99999.00
81	9	2008-10-29 00:00:00	2008-10-29 00:00:00	2008-10-29 00:00:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	\N	\N	Carsten Hollmann	text	-99999.00	-99999.00
82	10	2008-10-29 00:00:00	2008-10-29 00:00:00	2008-10-29 00:00:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	\N	\N	Christian Autermann	text	-99999.00	-99999.00
83	11	2008-10-29 00:00:00	2008-10-29 00:00:00	2008-10-29 00:00:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	\N	\N	Shane StClair	text	-99999.00	-99999.00
84	12	2012-12-31 23:00:00	2012-12-31 23:00:00	2012-12-31 22:01:00	\N	1	\N	1	\N	0	0	0	\N	\N	\N	\N	\N	\N	\N	John/Jane Doe	text	-99999.00	-99999.00
85	1	2012-11-19 13:00:00	2012-11-19 13:00:00	2012-11-19 13:00:00	http://www.52north.org/test/observation/1_firstValue_vertical_0	1	\N	1	\N	0	1	0	\N	\N	\N	\N	\N	\N	100	\N	quantity	10	-99999.00
86	1	2012-11-19 13:00:00	2012-11-19 13:00:00	2012-11-19 13:00:00	http://www.52north.org/test/observation/1_firstValue_vertical_1	1	\N	1	\N	0	1	0	\N	\N	\N	\N	\N	\N	200	\N	quantity	20	-99999.00
87	1	2012-11-19 13:09:00	2012-11-19 13:09:00	2012-11-19 13:09:00	http://www.52north.org/test/observation/1_lastValue_vertical_0	1	\N	1	\N	0	1	0	\N	\N	\N	\N	\N	\N	300	\N	quantity	15	-9999.00
88	1	2012-11-19 13:09:00	2012-11-19 13:09:00	2012-11-19 13:09:00	http://www.52north.org/test/observation/1_lastValue_vertical_1	1	\N	1	\N	0	1	0	\N	\N	\N	\N	\N	\N	400	\N	quantity	12	-9999.00
91	8	2012-11-19 13:00:00	2012-11-19 13:04:59	2012-11-19 13:00:00	http://www.52north.org/test/observation/8_91_resulttime_2012-11-19	1	\N	1	\N	0	0	0	\N	\N	0101000020E610000000000000000024400000000000002440	\N	\N	\N	5.0	\N	quantity	-9999.00	-9999.00
92	8	2012-11-19 13:05:00	2012-11-19 13:09:59	2012-11-19 13:00:00	http://www.52north.org/test/observation/8_92_resulttime_2012-11-19	1	\N	1	\N	0	0	0	\N	\N	0101000020E6100000CDCCCCCCCCCC1C40CDCCCCCCCC4C4940	\N	\N	\N	5.1	\N	quantity	-9999.00	-9999.00
93	8	2012-11-19 13:10:00	2012-11-19 13:14:59	2012-11-19 13:00:00	http://www.52north.org/test/observation/8_93_resulttime_2012-11-19	1	\N	1	\N	0	0	0	\N	\N	0101000020E61000003333333333331F40CDCCCCCCCCCC4940	\N	\N	\N	5.2	\N	quantity	-9999.00	-9999.00
94	8	2012-11-19 13:15:00	2012-11-19 13:19:59	2012-11-19 13:00:00	http://www.52north.org/test/observation/8_94_resulttime_2012-11-19	1	\N	1	\N	0	0	0	\N	\N	0101000020E6100000CDCCCCCCCCCC20409A99999999994940	\N	\N	\N	5.3	\N	quantity	-9999.00	-9999.00
95	8	2012-11-19 13:20:00	2012-11-19 13:24:59	2012-11-19 13:00:00	http://www.52north.org/test/observation/8_95_resulttime_2012-11-19	1	\N	1	\N	0	0	0	\N	\N	0101000020E61000009A99999999991D409A99999999194940	\N	\N	\N	5.4	\N	quantity	-9999.00	-9999.00
96	8	2012-11-19 13:25:00	2012-11-19 13:29:59	2012-11-19 13:00:00	http://www.52north.org/test/observation/8_96_resulttime_2012-11-19	1	\N	1	\N	0	0	0	\N	\N	0101000020E61000006666666666661E406666666666664940	\N	\N	\N	5.5	\N	quantity	-9999.00	-9999.00
97	8	2012-11-19 13:30:00	2012-11-19 13:34:59	2012-11-19 13:00:00	http://www.52north.org/test/observation/8_97_resulttime_2012-11-19	1	\N	1	\N	0	0	0	\N	\N	0101000020E610000000000000000020406666666666664940	\N	\N	\N	5.6	\N	quantity	-9999.00	-9999.00
98	8	2012-11-19 13:35:00	2012-11-19 13:39:59	2012-11-19 13:00:00	http://www.52north.org/test/observation/8_98_resulttime_2012-11-19	1	\N	1	\N	0	0	0	\N	\N	0101000020E610000000000000000021406666666666664940	\N	\N	\N	5.7	\N	quantity	-9999.00	-9999.00
99	8	2012-11-19 13:40:00	2012-11-19 13:44:59	2012-11-19 13:00:00	http://www.52north.org/test/observation/8_99_resulttime_2012-11-19	1	\N	1	\N	0	0	0	\N	\N	0101000020E610000033333333333320409A99999999194940	\N	\N	\N	5.8	\N	quantity	-9999.00	-9999.00
100	8	2012-11-19 13:45:00	2012-11-19 13:49:59	2012-11-19 13:00:00	http://www.52north.org/test/observation/8_100_resulttime_2012-11-19	1	\N	1	\N	0	0	0	\N	\N	0101000020E61000003333333333331F400000000000804940	\N	\N	\N	5.9	\N	quantity	-9999.00	-9999.00
\.

select updateGeometrySRID('observation','sampling_geometry',4326);



update dataset set fk_first_observation_id=1,fk_last_observation_id=10 where dataset_id=1;
update dataset set fk_first_observation_id=11,fk_last_observation_id=20 where dataset_id=2;
update dataset set fk_first_observation_id=21,fk_last_observation_id=30 where dataset_id=3;
update dataset set fk_first_observation_id=31,fk_last_observation_id=40 where dataset_id=4;
update dataset set fk_first_observation_id=41,fk_last_observation_id=50 where dataset_id=5;
update dataset set fk_first_observation_id=51,fk_last_observation_id=60,first_value=2.0,last_value=2.9 where dataset_id=6;
update dataset set fk_first_observation_id=61,fk_last_observation_id=70,first_value=3.0,last_value=3.9 where dataset_id=7;
update dataset set fk_first_observation_id=71,fk_last_observation_id=100,first_value=4.0,last_value=5.9 where dataset_id=8;
update dataset set fk_first_observation_id=81,fk_last_observation_id=81 where dataset_id=9;
update dataset set fk_first_observation_id=82,fk_last_observation_id=82 where dataset_id=10;
update dataset set fk_first_observation_id=83,fk_last_observation_id=83 where dataset_id=11;
update dataset set fk_first_observation_id=84,fk_last_observation_id=84 where dataset_id=12;



COPY parameter (parameter_id, name, type, value_quantity, fk_unit_id) FROM stdin;
1	test	quantity	10	2
2	test	quantity	20	2
3	test	quantity	15	2
4	test	quantity	12	2
\.



COPY observation_parameter (fk_parameter_id, fk_observation_id) FROM stdin;
1	51
2	52
3	53
4	54
\.



SELECT pg_catalog.setval('codespace_seq', 1, true);



SELECT pg_catalog.setval('feature_seq', 13, true);



SELECT pg_catalog.setval('format_seq', 8, true);



COPY feature_relation (fk_parent_feature_id, fk_child_feature_id) FROM stdin;
1	2
1	3
1	4
1	5
1	6
1	7
1	8
1	9
1	10
1	11
1	12
1	13
\.



SELECT pg_catalog.setval('category_seq', 9, true);


SELECT pg_catalog.setval('phenomenon_seq', 9, true);




SELECT pg_catalog.setval('observation_seq', 100, true);




COPY offering_feature_type (fk_offering_id, fk_format_id) FROM stdin;
1	1
2	1
3	1
4	1
5	1
6	1
7	1
8	1
9	1
\.



COPY offering_observation_type (fk_offering_id, fk_format_id) FROM stdin;
1	4
2	5
3	6
4	7
5	8
6	4
7	4
8	4
9	8
\.



SELECT pg_catalog.setval('offering_seq', 9, true);




SELECT pg_catalog.setval('parameter_seq', 4, true);




SELECT pg_catalog.setval('procedure_seq', 9, true);



COPY value_profile (fk_observation_id, vertical_from_name, vertical_to_name, fk_vertical_unit_id) FROM stdin;
1	depth	\N	6
10	depth	\N	6
\.



COPY composite_observation (fk_parent_observation_id, fk_child_observation_id) FROM stdin;
1	85
1	86
10	87
10	88
\.


SELECT pg_catalog.setval('related_feature_seq', 1, false);



SELECT pg_catalog.setval('related_feature_role_seq', 1, false);



SELECT pg_catalog.setval('related_observation_seq', 1, false);



COPY result_template (result_template_id, fk_offering_id, fk_phenomenon_id, fk_procedure_id, fk_feature_id, identifier, structure, encoding) FROM stdin;
1	6	6	6	7	http://www.52north.org/test/procedure/6/template/1	<swe:DataRecord xmlns:swe="http://www.opengis.net/swe/2.0" xmlns:xlink="http://www.w3.org/1999/xlink">\n  <swe:field name="phenomenonTime">\n    <swe:TimeRange definition="http://www.opengis.net/def/property/OGC/0/PhenomenonTime">\n      <swe:uom xlink:href="http://www.opengis.net/def/uom/ISO-8601/0/Gregorian"/>\n    </swe:TimeRange>\n  </swe:field>\n  <swe:field name="resultTime">\n    <swe:Time definition="http://www.opengis.net/def/property/OGC/0/ResultTime">\n      <swe:uom code="testunit1"/>\n    </swe:Time>\n  </swe:field>\n  <swe:field name="observable_property_6">\n    <swe:Quantity definition="http://www.52north.org/test/observableProperty/6">\n      <swe:uom code="test_unit_6"/>\n    </swe:Quantity>\n  </swe:field>\n</swe:DataRecord>	<swe:TextEncoding xmlns:swe="http://www.opengis.net/swe/2.0" blockSeparator="#" tokenSeparator=","/>
\.



SELECT pg_catalog.setval('result_template_seq', 1, true);



SELECT pg_catalog.setval('dataset_seq', 12, true);



SELECT pg_catalog.setval('unit_seq', 5, true);



COPY procedure_history (procedure_history_id, fk_procedure_id, fk_format_id, valid_from, valid_to, xml) FROM stdin;
1	1	3	2017-03-04 13:49:27.394	\N	<sml:SensorML xmlns:sml="http://www.opengis.net/sensorML/1.0.1" version="1.0.1">\n  <sml:member>\n    <sml:System xmlns:swe="http://www.opengis.net/swe/1.0.1" xmlns:swes="http://www.opengis.net/swes/2.0" xmlns:sos="http://www.opengis.net/sos/2.0" xmlns:gml="http://www.opengis.net/gml" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">\n      <sml:identification>\n        <sml:IdentifierList>\n          <sml:identifier name="uniqueID">\n            <sml:Term definition="urn:ogc:def:identifier:OGC:1.0:uniqueID">\n              <sml:value>http://www.52north.org/test/procedure/1</sml:value>\n            </sml:Term>\n          </sml:identifier>\n          <sml:identifier name="longName">\n            <sml:Term definition="urn:ogc:def:identifier:OGC:1.0:longName">\n              <sml:value>con terra GmbH (www.conterra.de)</sml:value>\n            </sml:Term>\n          </sml:identifier>\n          <sml:identifier name="shortName">\n            <sml:Term definition="urn:ogc:def:identifier:OGC:1.0:shortName">\n              <sml:value>con terra</sml:value>\n            </sml:Term>\n          </sml:identifier>\n        </sml:IdentifierList>\n      </sml:identification>\n      <sml:capabilities name="featuresOfInterest">\n        <swe:SimpleDataRecord>\n          <swe:field name="featureOfInterestID">\n            <swe:Text>\n              <swe:value>http://www.52north.org/test/featureOfInterest/1</swe:value>\n            </swe:Text>\n          </swe:field>\n        </swe:SimpleDataRecord>\n      </sml:capabilities>\n      <sml:position name="sensorPosition">\n        <swe:Position referenceFrame="urn:ogc:def:crs:EPSG::4326">\n          <swe:location>\n            <swe:Vector gml:id="STATION_LOCATION">\n              <swe:coordinate name="easting">\n                <swe:Quantity axisID="x">\n                  <swe:uom code="degree"/>\n                  <swe:value>7.727958</swe:value>\n                </swe:Quantity>\n              </swe:coordinate>\n              <swe:coordinate name="northing">\n                <swe:Quantity axisID="y">\n                  <swe:uom code="degree"/>\n                  <swe:value>51.883906</swe:value>\n                </swe:Quantity>\n              </swe:coordinate>\n              <swe:coordinate name="altitude">\n                <swe:Quantity axisID="z">\n                  <swe:uom code="m"/>\n                  <swe:value>52.0</swe:value>\n                </swe:Quantity>\n              </swe:coordinate>\n            </swe:Vector>\n          </swe:location>\n        </swe:Position>\n      </sml:position>\n      <sml:inputs>\n        <sml:InputList>\n          <sml:input name="test_observable_property_1">\n            <swe:ObservableProperty definition="http://www.52north.org/test/observableProperty/1"/>\n          </sml:input>\n        </sml:InputList>\n      </sml:inputs>\n      <sml:outputs>\n        <sml:OutputList>\n          <sml:output name="test_observable_property_1">\n            <swe:Category definition="http://www.52north.org/test/observableProperty/1">\n              <swe:codeSpace xlink:href="test_unit_1"/>\n            </swe:Category>\n          </sml:output>\n        </sml:OutputList>\n      </sml:outputs>\n    </sml:System>\n  </sml:member>\n</sml:SensorML>
2	2	3	2017-03-04 13:49:27.951	\N	<sml:SensorML xmlns:sml="http://www.opengis.net/sensorML/1.0.1" version="1.0.1">\n  <sml:member>\n    <sml:System xmlns:swe="http://www.opengis.net/swe/1.0.1" xmlns:swes="http://www.opengis.net/swes/2.0" xmlns:sos="http://www.opengis.net/sos/2.0" xmlns:gml="http://www.opengis.net/gml" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">\n      <sml:identification>\n        <sml:IdentifierList>\n          <sml:identifier name="uniqueID">\n            <sml:Term definition="urn:ogc:def:identifier:OGC:1.0:uniqueID">\n              <sml:value>http://www.52north.org/test/procedure/2</sml:value>\n            </sml:Term>\n          </sml:identifier>\n          <sml:identifier name="longName">\n            <sml:Term definition="urn:ogc:def:identifier:OGC:1.0:longName">\n              <sml:value>ESRI (www.esri.com)</sml:value>\n            </sml:Term>\n          </sml:identifier>\n          <sml:identifier name="shortName">\n            <sml:Term definition="urn:ogc:def:identifier:OGC:1.0:shortName">\n              <sml:value>ESRI</sml:value>\n            </sml:Term>\n          </sml:identifier>\n        </sml:IdentifierList>\n      </sml:identification>\n      <sml:capabilities name="featuresOfInterest">\n        <swe:SimpleDataRecord>\n          <swe:field name="featureOfInterestID">\n            <swe:Text>\n              <swe:value>http://www.52north.org/test/featureOfInterest/2</swe:value>\n            </swe:Text>\n          </swe:field>\n        </swe:SimpleDataRecord>\n      </sml:capabilities>\n      <sml:position name="sensorPosition">\n        <swe:Position referenceFrame="urn:ogc:def:crs:EPSG::4326">\n          <swe:location>\n            <swe:Vector gml:id="STATION_LOCATION">\n              <swe:coordinate name="easting">\n                <swe:Quantity axisID="x">\n                  <swe:uom code="degree"/>\n                  <swe:value>-117.195711</swe:value>\n                </swe:Quantity>\n              </swe:coordinate>\n              <swe:coordinate name="northing">\n                <swe:Quantity axisID="y">\n                  <swe:uom code="degree"/>\n                  <swe:value>34.056517</swe:value>\n                </swe:Quantity>\n              </swe:coordinate>\n              <swe:coordinate name="altitude">\n                <swe:Quantity axisID="z">\n                  <swe:uom code="m"/>\n                  <swe:value>52.0</swe:value>\n                </swe:Quantity>\n              </swe:coordinate>\n            </swe:Vector>\n          </swe:location>\n        </swe:Position>\n      </sml:position>\n      <sml:inputs>\n        <sml:InputList>\n          <sml:input name="test_observable_property_2">\n            <swe:ObservableProperty definition="http://www.52north.org/test/observableProperty/2"/>\n          </sml:input>\n        </sml:InputList>\n      </sml:inputs>\n      <sml:outputs>\n        <sml:OutputList>\n          <sml:output name="test_observable_property_2">\n            <swe:Count definition="http://www.52north.org/test/observableProperty/2"/>\n          </sml:output>\n        </sml:OutputList>\n      </sml:outputs>\n    </sml:System>\n  </sml:member>\n</sml:SensorML>
3	3	3	2017-03-04 13:49:28.117	\N	<sml:SensorML xmlns:sml="http://www.opengis.net/sensorML/1.0.1" version="1.0.1">\n  <sml:member>\n    <sml:System xmlns:swe="http://www.opengis.net/swe/1.0.1" xmlns:swes="http://www.opengis.net/swes/2.0" xmlns:sos="http://www.opengis.net/sos/2.0" xmlns:gml="http://www.opengis.net/gml" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">\n      <sml:identification>\n        <sml:IdentifierList>\n          <sml:identifier name="uniqueID">\n            <sml:Term definition="urn:ogc:def:identifier:OGC:1.0:uniqueID">\n              <sml:value>http://www.52north.org/test/procedure/3</sml:value>\n            </sml:Term>\n          </sml:identifier>\n          <sml:identifier name="longName">\n            <sml:Term definition="urn:ogc:def:identifier:OGC:1.0:longName">\n              <sml:value>Kisters AG (www.kisters.de)</sml:value>\n            </sml:Term>\n          </sml:identifier>\n          <sml:identifier name="shortName">\n            <sml:Term definition="urn:ogc:def:identifier:OGC:1.0:shortName">\n              <sml:value>Kisters</sml:value>\n            </sml:Term>\n          </sml:identifier>\n        </sml:IdentifierList>\n      </sml:identification>\n      <sml:capabilities name="featuresOfInterest">\n        <swe:SimpleDataRecord>\n          <swe:field name="featureOfInterestID">\n            <swe:Text>\n              <swe:value>http://www.52north.org/test/featureOfInterest/3</swe:value>\n            </swe:Text>\n          </swe:field>\n        </swe:SimpleDataRecord>\n      </sml:capabilities>\n      <sml:position name="sensorPosition">\n        <swe:Position referenceFrame="urn:ogc:def:crs:EPSG::4326">\n          <swe:location>\n            <swe:Vector gml:id="STATION_LOCATION">\n              <swe:coordinate name="easting">\n                <swe:Quantity axisID="x">\n                  <swe:uom code="degree"/>\n                  <swe:value>6.1320144042060925</swe:value>\n                </swe:Quantity>\n              </swe:coordinate>\n              <swe:coordinate name="northing">\n                <swe:Quantity axisID="y">\n                  <swe:uom code="degree"/>\n                  <swe:value>50.78570661296184</swe:value>\n                </swe:Quantity>\n              </swe:coordinate>\n              <swe:coordinate name="altitude">\n                <swe:Quantity axisID="z">\n                  <swe:uom code="m"/>\n                  <swe:value>52.0</swe:value>\n                </swe:Quantity>\n              </swe:coordinate>\n            </swe:Vector>\n          </swe:location>\n        </swe:Position>\n      </sml:position>\n      <sml:inputs>\n        <sml:InputList>\n          <sml:input name="test_observable_property_3">\n            <swe:ObservableProperty definition="http://www.52north.org/test/observableProperty/3"/>\n          </sml:input>\n        </sml:InputList>\n      </sml:inputs>\n      <sml:outputs>\n        <sml:OutputList>\n          <sml:output name="test_observable_property_3">\n            <swe:Boolean definition="http://www.52north.org/test/observableProperty/3"/>\n          </sml:output>\n        </sml:OutputList>\n      </sml:outputs>\n    </sml:System>\n  </sml:member>\n</sml:SensorML>
4	4	3	2017-03-04 13:49:28.281	\N	<sml:SensorML xmlns:sml="http://www.opengis.net/sensorML/1.0.1" version="1.0.1">\n  <sml:member>\n    <sml:System xmlns:swe="http://www.opengis.net/swe/1.0.1" xmlns:swes="http://www.opengis.net/swes/2.0" xmlns:sos="http://www.opengis.net/sos/2.0" xmlns:gml="http://www.opengis.net/gml" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">\n      <sml:identification>\n        <sml:IdentifierList>\n          <sml:identifier name="uniqueID">\n            <sml:Term definition="urn:ogc:def:identifier:OGC:1.0:uniqueID">\n              <sml:value>http://www.52north.org/test/procedure/4</sml:value>\n            </sml:Term>\n          </sml:identifier>\n          <sml:identifier name="longName">\n            <sml:Term definition="urn:ogc:def:identifier:OGC:1.0:longName">\n              <sml:value>Institute for Geoinformatics (http://ifgi.uni-muenster.de/en)</sml:value>\n            </sml:Term>\n          </sml:identifier>\n          <sml:identifier name="shortName">\n            <sml:Term definition="urn:ogc:def:identifier:OGC:1.0:shortName">\n              <sml:value>IfGI</sml:value>\n            </sml:Term>\n          </sml:identifier>\n        </sml:IdentifierList>\n      </sml:identification>\n      <sml:capabilities name="featuresOfInterest">\n        <swe:SimpleDataRecord>\n          <swe:field name="featureOfInterestID">\n            <swe:Text>\n              <swe:value>http://www.52north.org/test/featureOfInterest/4</swe:value>\n            </swe:Text>\n          </swe:field>\n        </swe:SimpleDataRecord>\n      </sml:capabilities>\n      <sml:position name="sensorPosition">\n        <swe:Position referenceFrame="urn:ogc:def:crs:EPSG::4326">\n          <swe:location>\n            <swe:Vector gml:id="STATION_LOCATION">\n              <swe:coordinate name="easting">\n                <swe:Quantity axisID="x">\n                  <swe:uom code="degree"/>\n                  <swe:value>7.593655600000034</swe:value>\n                </swe:Quantity>\n              </swe:coordinate>\n              <swe:coordinate name="northing">\n                <swe:Quantity axisID="y">\n                  <swe:uom code="degree"/>\n                  <swe:value>51.9681661</swe:value>\n                </swe:Quantity>\n              </swe:coordinate>\n              <swe:coordinate name="altitude">\n                <swe:Quantity axisID="z">\n                  <swe:uom code="m"/>\n                  <swe:value>52.0</swe:value>\n                </swe:Quantity>\n              </swe:coordinate>\n            </swe:Vector>\n          </swe:location>\n        </swe:Position>\n      </sml:position>\n      <sml:inputs>\n        <sml:InputList>\n          <sml:input name="test_observable_property_4">\n            <swe:ObservableProperty definition="http://www.52north.org/test/observableProperty/4"/>\n          </sml:input>\n        </sml:InputList>\n      </sml:inputs>\n      <sml:outputs>\n        <sml:OutputList>\n          <sml:output name="test_observable_property_4">\n            <swe:Quantity definition="http://www.52north.org/test/observableProperty/4">\n              <swe:uom code="test_unit_4"/>\n            </swe:Quantity>\n          </sml:output>\n        </sml:OutputList>\n      </sml:outputs>\n    </sml:System>\n  </sml:member>\n</sml:SensorML>
5	5	3	2017-03-04 13:49:28.441	\N	<sml:SensorML xmlns:sml="http://www.opengis.net/sensorML/1.0.1" version="1.0.1">\n  <sml:member>\n    <sml:System xmlns:swe="http://www.opengis.net/swe/1.0.1" xmlns:swes="http://www.opengis.net/swes/2.0" xmlns:sos="http://www.opengis.net/sos/2.0" xmlns:gml="http://www.opengis.net/gml" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">\n      <sml:identification>\n        <sml:IdentifierList>\n          <sml:identifier name="uniqueID">\n            <sml:Term definition="urn:ogc:def:identifier:OGC:1.0:uniqueID">\n              <sml:value>http://www.52north.org/test/procedure/5</sml:value>\n            </sml:Term>\n          </sml:identifier>\n          <sml:identifier name="longName">\n            <sml:Term definition="urn:ogc:def:identifier:OGC:1.0:longName">\n              <sml:value>Technical University Dresden (http://tu-dresden.de/en)</sml:value>\n            </sml:Term>\n          </sml:identifier>\n          <sml:identifier name="shortName">\n            <sml:Term definition="urn:ogc:def:identifier:OGC:1.0:shortName">\n              <sml:value>TU-Dresden</sml:value>\n            </sml:Term>\n          </sml:identifier>\n        </sml:IdentifierList>\n      </sml:identification>\n      <sml:capabilities name="featuresOfInterest">\n        <swe:SimpleDataRecord>\n          <swe:field name="featureOfInterestID">\n            <swe:Text>\n              <swe:value>http://www.52north.org/test/featureOfInterest/5</swe:value>\n            </swe:Text>\n          </swe:field>\n        </swe:SimpleDataRecord>\n      </sml:capabilities>\n      <sml:position name="sensorPosition">\n        <swe:Position referenceFrame="urn:ogc:def:crs:EPSG::4326">\n          <swe:location>\n            <swe:Vector gml:id="STATION_LOCATION">\n              <swe:coordinate name="easting">\n                <swe:Quantity axisID="x">\n                  <swe:uom code="degree"/>\n                  <swe:value>13.72375999999997</swe:value>\n                </swe:Quantity>\n              </swe:coordinate>\n              <swe:coordinate name="northing">\n                <swe:Quantity axisID="y">\n                  <swe:uom code="degree"/>\n                  <swe:value>51.02881</swe:value>\n                </swe:Quantity>\n              </swe:coordinate>\n              <swe:coordinate name="altitude">\n                <swe:Quantity axisID="z">\n                  <swe:uom code="m"/>\n                  <swe:value>52.0</swe:value>\n                </swe:Quantity>\n              </swe:coordinate>\n            </swe:Vector>\n          </swe:location>\n        </swe:Position>\n      </sml:position>\n      <sml:inputs>\n        <sml:InputList>\n          <sml:input name="test_observable_property_5">\n            <swe:ObservableProperty definition="http://www.52north.org/test/observableProperty/5"/>\n          </sml:input>\n        </sml:InputList>\n      </sml:inputs>\n      <sml:outputs>\n        <sml:OutputList>\n          <sml:output name="test_observable_property_5">\n            <swe:Text definition="http://www.52north.org/test/observableProperty/5"/>\n          </sml:output>\n        </sml:OutputList>\n      </sml:outputs>\n    </sml:System>\n  </sml:member>\n</sml:SensorML>
6	6	3	2017-03-04 13:49:28.575	\N	<sml:SensorML xmlns:sml="http://www.opengis.net/sensorML/1.0.1" version="1.0.1">\n  <sml:member>\n    <sml:System xmlns:swe="http://www.opengis.net/swe/1.0.1" xmlns:swes="http://www.opengis.net/swes/2.0" xmlns:sos="http://www.opengis.net/sos/2.0" xmlns:gml="http://www.opengis.net/gml" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">\n      <sml:identification>\n        <sml:IdentifierList>\n          <sml:identifier name="uniqueID">\n            <sml:Term definition="urn:ogc:def:identifier:OGC:1.0:uniqueID">\n              <sml:value>http://www.52north.org/test/procedure/6</sml:value>\n            </sml:Term>\n          </sml:identifier>\n          <sml:identifier name="longName">\n            <sml:Term definition="urn:ogc:def:identifier:OGC:1.0:longName">\n              <sml:value>Hochschule Bochum - Bochum University of Applied Sciences (http://www.hochschule-bochum.de/en/)</sml:value>\n            </sml:Term>\n          </sml:identifier>\n          <sml:identifier name="shortName">\n            <sml:Term definition="urn:ogc:def:identifier:OGC:1.0:shortName">\n              <sml:value>Hochschule Bochum</sml:value>\n            </sml:Term>\n          </sml:identifier>\n        </sml:IdentifierList>\n      </sml:identification>\n      <sml:capabilities name="featuresOfInterest">\n        <swe:SimpleDataRecord>\n          <swe:field name="featureOfInterestID">\n            <swe:Text>\n              <swe:value>http://www.52north.org/test/featureOfInterest/6</swe:value>\n            </swe:Text>\n          </swe:field>\n        </swe:SimpleDataRecord>\n      </sml:capabilities>\n      <sml:position name="sensorPosition">\n        <swe:Position referenceFrame="urn:ogc:def:crs:EPSG::4326">\n          <swe:location>\n            <swe:Vector gml:id="STATION_LOCATION">\n              <swe:coordinate name="easting">\n                <swe:Quantity axisID="x">\n                  <swe:uom code="degree"/>\n                  <swe:value>7.270806</swe:value>\n                </swe:Quantity>\n              </swe:coordinate>\n              <swe:coordinate name="northing">\n                <swe:Quantity axisID="y">\n                  <swe:uom code="degree"/>\n                  <swe:value>51.447722</swe:value>\n                </swe:Quantity>\n              </swe:coordinate>\n              <swe:coordinate name="altitude">\n                <swe:Quantity axisID="z">\n                  <swe:uom code="m"/>\n                  <swe:value>52.0</swe:value>\n                </swe:Quantity>\n              </swe:coordinate>\n            </swe:Vector>\n          </swe:location>\n        </swe:Position>\n      </sml:position>\n      <sml:inputs>\n        <sml:InputList>\n          <sml:input name="test_observable_property_6">\n            <swe:ObservableProperty definition="http://www.52north.org/test/observableProperty/6"/>\n          </sml:input>\n        </sml:InputList>\n      </sml:inputs>\n      <sml:outputs>\n        <sml:OutputList>\n          <sml:output name="test_observable_property_6">\n            <swe:Category definition="http://www.52north.org/test/observableProperty/6">\n              <swe:codeSpace xlink:href="test_unit_6"/>\n            </swe:Category>\n          </sml:output>\n        </sml:OutputList>\n      </sml:outputs>\n    </sml:System>\n  </sml:member>\n</sml:SensorML>
7	7	3	2017-03-04 13:49:28.826	\N	<sml:SensorML xmlns:sml="http://www.opengis.net/sensorML/1.0.1" version="1.0.1">\n  <sml:member>\n    <sml:System xmlns:swe="http://www.opengis.net/swe/1.0.1" xmlns:swes="http://www.opengis.net/swes/2.0" xmlns:sos="http://www.opengis.net/sos/2.0" xmlns:gml="http://www.opengis.net/gml" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">\n      <sml:identification>\n        <sml:IdentifierList>\n          <sml:identifier name="uniqueID">\n            <sml:Term definition="urn:ogc:def:identifier:OGC:1.0:uniqueID">\n              <sml:value>http://www.52north.org/test/procedure/7</sml:value>\n            </sml:Term>\n          </sml:identifier>\n          <sml:identifier name="longName">\n            <sml:Term definition="urn:ogc:def:identifier:OGC:1.0:longName">\n              <sml:value>ITC - University of Twente (http://www.itc.nl/)</sml:value>\n            </sml:Term>\n          </sml:identifier>\n          <sml:identifier name="shortName">\n            <sml:Term definition="urn:ogc:def:identifier:OGC:1.0:shortName">\n              <sml:value>ITC</sml:value>\n            </sml:Term>\n          </sml:identifier>\n        </sml:IdentifierList>\n      </sml:identification>\n      <sml:capabilities name="featuresOfInterest">\n        <swe:SimpleDataRecord>\n          <swe:field name="featureOfInterestID">\n            <swe:Text>\n              <swe:value>http://www.52north.org/test/featureOfInterest/7</swe:value>\n            </swe:Text>\n          </swe:field>\n        </swe:SimpleDataRecord>\n      </sml:capabilities>\n      <sml:position name="sensorPosition">\n        <swe:Position referenceFrame="urn:ogc:def:crs:EPSG::4326">\n          <swe:location>\n            <swe:Vector gml:id="STATION_LOCATION">\n              <swe:coordinate name="easting">\n                <swe:Quantity axisID="x">\n                  <swe:uom code="degree"/>\n                  <swe:value>4.283393599999954</swe:value>\n                </swe:Quantity>\n              </swe:coordinate>\n              <swe:coordinate name="northing">\n                <swe:Quantity axisID="y">\n                  <swe:uom code="degree"/>\n                  <swe:value>52.0464393</swe:value>\n                </swe:Quantity>\n              </swe:coordinate>\n              <swe:coordinate name="altitude">\n                <swe:Quantity axisID="z">\n                  <swe:uom code="m"/>\n                  <swe:value>52.0</swe:value>\n                </swe:Quantity>\n              </swe:coordinate>\n            </swe:Vector>\n          </swe:location>\n        </swe:Position>\n      </sml:position>\n      <sml:inputs>\n        <sml:InputList>\n          <sml:input name="test_observable_property_7">\n            <swe:ObservableProperty definition="http://www.52north.org/test/observableProperty/7"/>\n          </sml:input>\n        </sml:InputList>\n      </sml:inputs>\n      <sml:outputs>\n        <sml:OutputList>\n          <sml:output name="test_observable_property_7">\n            <swe:Category definition="http://www.52north.org/test/observableProperty/7">\n              <swe:codeSpace xlink:href="test_unit_7"/>\n            </swe:Category>\n          </sml:output>\n        </sml:OutputList>\n      </sml:outputs>\n    </sml:System>\n  </sml:member>\n</sml:SensorML>
8	8	3	2017-03-04 13:49:28.965	\N	<sml:SensorML xmlns:sml="http://www.opengis.net/sensorML/1.0.1" version="1.0.1">\n  <sml:member>\n    <sml:System xmlns:swe="http://www.opengis.net/swe/1.0.1" xmlns:swes="http://www.opengis.net/swes/2.0" xmlns:sos="http://www.opengis.net/sos/2.0" xmlns:gml="http://www.opengis.net/gml" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">\n      <sml:identification>\n        <sml:IdentifierList>\n          <sml:identifier name="uniqueID">\n            <sml:Term definition="urn:ogc:def:identifier:OGC:1.0:uniqueID">\n              <sml:value>http://www.52north.org/test/procedure/8</sml:value>\n            </sml:Term>\n          </sml:identifier>\n          <sml:identifier name="longName">\n            <sml:Term definition="urn:ogc:def:identifier:OGC:1.0:longName">\n              <sml:value>Bundesanstalt für IT-Dienstleistungen im Geschäftsbereich des BMVBS (http://www.dlz-it.de)</sml:value>\n            </sml:Term>\n          </sml:identifier>\n          <sml:identifier name="shortName">\n            <sml:Term definition="urn:ogc:def:identifier:OGC:1.0:shortName">\n              <sml:value>DLZ-IT</sml:value>\n            </sml:Term>\n          </sml:identifier>\n        </sml:IdentifierList>\n      </sml:identification>\n      <sml:capabilities name="featuresOfInterest">\n        <swe:SimpleDataRecord>\n          <swe:field name="featureOfInterestID">\n            <swe:Text>\n              <swe:value>http://www.52north.org/test/featureOfInterest/8</swe:value>\n            </swe:Text>\n          </swe:field>\n        </swe:SimpleDataRecord>\n      </sml:capabilities>\n      <sml:position name="sensorPosition">\n        <swe:Position referenceFrame="urn:ogc:def:crs:EPSG::4326">\n          <swe:location>\n            <swe:Vector gml:id="STATION_LOCATION">\n              <swe:coordinate name="easting">\n                <swe:Quantity axisID="x">\n                  <swe:uom code="degree"/>\n                  <swe:value>10.94306000000006</swe:value>\n                </swe:Quantity>\n              </swe:coordinate>\n              <swe:coordinate name="northing">\n                <swe:Quantity axisID="y">\n                  <swe:uom code="degree"/>\n                  <swe:value>50.68606</swe:value>\n                </swe:Quantity>\n              </swe:coordinate>\n              <swe:coordinate name="altitude">\n                <swe:Quantity axisID="z">\n                  <swe:uom code="m"/>\n                  <swe:value>52.0</swe:value>\n                </swe:Quantity>\n              </swe:coordinate>\n            </swe:Vector>\n          </swe:location>\n        </swe:Position>\n      </sml:position>\n      <sml:inputs>\n        <sml:InputList>\n          <sml:input name="test_observable_property_8">\n            <swe:ObservableProperty definition="http://www.52north.org/test/observableProperty/8"/>\n          </sml:input>\n        </sml:InputList>\n      </sml:inputs>\n      <sml:outputs>\n        <sml:OutputList>\n          <sml:output name="test_observable_property_8">\n            <swe:Category definition="http://www.52north.org/test/observableProperty/8">\n              <swe:codeSpace xlink:href="test_unit_8"/>\n            </swe:Category>\n          </sml:output>\n        </sml:OutputList>\n      </sml:outputs>\n    </sml:System>\n  </sml:member>\n</sml:SensorML>
9	9	3	2017-03-04 13:49:29.263	\N	<sml:SensorML xmlns:sml="http://www.opengis.net/sensorML/1.0.1" version="1.0.1">\n  <sml:member>\n    <sml:ProcessModel xmlns:swe="http://www.opengis.net/swe/1.0.1" xmlns:gml="http://www.opengis.net/gml" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" gml:id="developer">\n      <gml:description>52°North developer process</gml:description>\n      <gml:name>http://www.52north.org/test/procedure/developer</gml:name>\n      <sml:identification>\n        <sml:IdentifierList>\n          <sml:identifier name="uniqueID">\n            <sml:Term definition="urn:ogc:def:identifier:OGC:uniqueID">\n              <sml:value>http://www.52north.org/test/procedure/developer</sml:value>\n            </sml:Term>\n          </sml:identifier>\n        </sml:IdentifierList>\n      </sml:identification>\n      <sml:inputs>\n        <sml:InputList>\n          <sml:input name="community">\n            <swe:Text definition="http://www.52north.org/test/observableProperty/community"/>\n          </sml:input>\n        </sml:InputList>\n      </sml:inputs>\n      <sml:outputs>\n        <sml:OutputList>\n          <sml:output name="developer">\n            <swe:Text definition="http://www.52north.org/test/observableProperty/developer"/>\n          </sml:output>\n        </sml:OutputList>\n      </sml:outputs>\n      <sml:method>\n        <sml:ProcessMethod>\n          <gml:description>52deg;North Sensor Observation Service development method</gml:description>\n          <sml:contact xlink:arcrole="author">\n            <sml:ResponsibleParty>\n              <sml:individualName>Carsten Hollmann</sml:individualName>\n              <sml:organizationName>52deg;North Initiative for Geospatial Open Source Software GmbH</sml:organizationName>\n              <sml:positionName>code manager</sml:positionName>\n              <sml:contactInfo>\n                <sml:phone>\n                  <sml:voice>+49 (0)251 396371 0</sml:voice>\n                </sml:phone>\n                <sml:address>\n                  <sml:deliveryPoint>Martin-Luther-King-Weg 24</sml:deliveryPoint>\n                  <sml:city>Muenster</sml:city>\n                  <sml:administrativeArea>North Rhine-Westphalia</sml:administrativeArea>\n                  <sml:postalCode>48155</sml:postalCode>\n                  <sml:country>Germany</sml:country>\n                  <sml:electronicMailAddress>info@52north.org</sml:electronicMailAddress>\n                </sml:address>\n              </sml:contactInfo>\n            </sml:ResponsibleParty>\n          </sml:contact>\n          <sml:rules>\n            <sml:RulesDefinition>\n              <gml:description>Inputs are the specifications and the idea, Output is the SOS service</gml:description>\n            </sml:RulesDefinition>\n          </sml:rules>\n          <sml:algorithm>\n            <sml:AlgorithmDefinition>\n              <gml:description>Spec/Idea to code</gml:description>\n            </sml:AlgorithmDefinition>\n          </sml:algorithm>\n        </sml:ProcessMethod>\n      </sml:method>\n    </sml:ProcessModel>\n  </sml:member>\n</sml:SensorML>
\.


SELECT pg_catalog.setval('procedure_history_seq', 9, true);



-- Completed on 2017-07-18 15:11:04 CEST

--
-- PostgreSQL database dump complete
--

