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


COPY feature (feature_id, discriminator, identifier, name, description, geom) FROM stdin;
1	\N	http://www.52north.org/test/featureOfInterest/world	\N	\N	\N
2	\N	http://www.52north.org/test/featureOfInterest/1	con terra	\N	0101000020E61000003F726BD26DE91E407D5EF1D423F14940
3	\N	http://www.52north.org/test/featureOfInterest/2	ESRI	\N	0101000020E6100000EB1D6E87864C5DC08255F5F23B074140
4	\N	http://www.52north.org/test/featureOfInterest/3	Kisters	\N	0101000020E610000014AAB2C82E8718400576C70892644940
5	\N	http://www.52north.org/test/featureOfInterest/4	IfGI	\N	0101000020E61000003F726BD26DE91E407D5EF1D423F14940
6	\N	http://www.52north.org/test/featureOfInterest/5	TU-Dresden	\N	0101000020E6100000404EB4AB90722B401DE6CB0BB0834940
7	\N	http://www.52north.org/test/featureOfInterest/6	Hochschule Bochum	\N	0101000020E6100000083E062B4E151D4090D959F44EB94940
8	\N	http://www.52north.org/test/featureOfInterest/7	ITC	\N	0101000020E610000000958FEE31221140E45F15B9F1054A40
9	\N	http://www.52north.org/test/featureOfInterest/8	DLZ-IT	\N	\N
10	\N	http://www.52north.org/test/featureOfInterest/Heiden	Heiden	\N	0101000020E61000008C118942CBBA1B404D874ECFBBE94940
11	\N	http://www.52north.org/test/featureOfInterest/Münster/FE101	Münster/FE101	\N	0101000020E610000099B9C0E5B1861E405473B9C150F94940
12	\N	http://www.52north.org/test/featureOfInterest/Portland	Portland	\N	0101000020E61000001DC9E53FA4AB5EC0C3F5285C8FC24640
13	\N	http://www.52north.org/test/featureOfInterest/TODO	TODO	\N	0101000020E610000000000000000000000000000000000000
\.



COPY phenomenon (phenomenon_id, identifier, name, description) FROM stdin;
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




COPY offering (offering_id, identifier, name, description, sampling_time_start, sampling_time_end, result_time_start, result_time_end) FROM stdin;
1	http://www.52north.org/test/offering/1	Offering for sensor 1	\N	1970-01-01 00:00:00	1970-01-01 00:00:00	1970-01-01 00:00:00	1970-01-01 00:00:00
2	http://www.52north.org/test/offering/2	Offering for sensor 2	\N	1970-01-01 00:00:00	1970-01-01 00:00:00	1970-01-01 00:00:00	1970-01-01 00:00:00
3	http://www.52north.org/test/offering/3	Offering for sensor 3	\N	1970-01-01 00:00:00	1970-01-01 00:00:00	1970-01-01 00:00:00	1970-01-01 00:00:00
4	http://www.52north.org/test/offering/4	Offering for sensor 2	\N	1970-01-01 00:00:00	1970-01-01 00:00:00	1970-01-01 00:00:00	1970-01-01 00:00:00
5	http://www.52north.org/test/offering/5	Offering for sensor 5	\N	1970-01-01 00:00:00	1970-01-01 00:00:00	1970-01-01 00:00:00	1970-01-01 00:00:00
6	http://www.52north.org/test/offering/6	Offering for sensor 6	\N	1970-01-01 00:00:00	1970-01-01 00:00:00	1970-01-01 00:00:00	1970-01-01 00:00:00
7	http://www.52north.org/test/offering/7	Offering for sensor 7	\N	1970-01-01 00:00:00	1970-01-01 00:00:00	1970-01-01 00:00:00	1970-01-01 00:00:00
8	http://www.52north.org/test/offering/8	Offering for sensor 8	\N	1970-01-01 00:00:00	1970-01-01 00:00:00	1970-01-01 00:00:00	1970-01-01 00:00:00
9	http://www.52north.org/test/offering/developer	Offering for procedure developer	\N	1970-01-01 00:00:00	1970-01-01 00:00:00	1970-01-01 00:00:00	1970-01-01 00:00:00
\.



COPY platform (platform_id, identifier, name, description) FROM stdin;
1	http://www.52north.org/test/procedure/1	con terra	\N
2	http://www.52north.org/test/procedure/2	ESRI	\N
3	http://www.52north.org/test/procedure/3	Kisters	\N
4	http://www.52north.org/test/procedure/4	IfGI	\N
5	http://www.52north.org/test/procedure/5	TU-Dresden	\N
6	http://www.52north.org/test/procedure/6	Hochschule Bochum	\N
7	http://www.52north.org/test/procedure/7	ITC	\N
8	http://www.52north.org/test/procedure/8	DLZ-IT	\N
9	http://www.52north.org/test/procedure/developer	\N	http://www.52north.org/test/procedure/developer	\N
10	http://www.52north.org/test/procedure/reference	\N	http://www.52north.org/test/procedure/reference	\N
\.



COPY procedure (procedure_id, identifier, name, description, is_deleted, description_file, is_reference) FROM stdin;
1	http://www.52north.org/test/procedure/1	con terra	\N	0	\N	0
2	http://www.52north.org/test/procedure/2	ESRI	\N	0	\N	0
3	http://www.52north.org/test/procedure/3	Kisters	\N	0	\N	0
4	http://www.52north.org/test/procedure/4	IfGI	\N	0	\N	0
5	http://www.52north.org/test/procedure/5	TU-Dresden	\N	1	\N	0
6	http://www.52north.org/test/procedure/6	Hochschule Bochum	\N	0	\N	0
7	http://www.52north.org/test/procedure/7	ITC	\N	0	\N	0
8	http://www.52north.org/test/procedure/8	DLZ-IT	\N	0	\N	0
9	http://www.52north.org/test/procedure/developer	http://www.52north.org/test/procedure/developer	\N	0	\N	0
10	http://www.52north.org/test/procedure/reference	http://www.52north.org/test/procedure/reference	\N	0	\N	1
\.



COPY unit (unit_id, symbol, name, link) FROM stdin;
1	test_unit_1	\N	\N
2	test_unit_4	\N	\N
3	test_unit_6	\N	\N
4	test_unit_7	\N	\N
5	test_unit_8	\N	\N
6	m	\N	\N
\.



COPY dataset (dataset_id, fk_feature_id, fk_category_id, fk_phenomenon_id, fk_procedure_id, fk_platform_id, fk_format_id, fk_offering_id, is_deleted, is_published, is_hidden, first_time, last_time, fk_unit_id, identifier, fk_identifier_codespace_id, name, fk_name_codespace_id, description, dataset_type, aggregation_type, value_type, decimals, is_mobile, is_insitu) FROM stdin;
1	2	1	1	1	1	4	1	0	1	0	2012-11-19 13:00:00	2012-11-19 13:09:00	1	\N	\N	\N	\N	\N	timeseries	profile	quantity	3	0	1
2	3	2	2	2	2	5	2	0	1	0	2012-11-19 13:00:00	2012-11-19 13:09:00	\N	\N	\N	\N	\N	\N	timeseries	discrete	count	\N	0	1
3	4	3	3	3	3	6	3	0	1	0	2012-11-19 13:00:00	2012-11-19 13:09:00	\N	\N	\N	\N	\N	\N	timeseries	discrete	bool	\N	0	1
4	5	4	4	4	4	7	4	0	1	0	2012-11-19 13:00:00	2012-11-19 13:09:00	\N	\N	\N	\N	\N	\N	timeseries	discrete	category	\N	0	1
5	6	5	5	5	5	8	5	1	1	0	2012-11-19 13:00:00	2012-11-19 13:09:00	\N	\N	\N	\N	\N	\N	timeseries	discrete	text	\N	0	1
6	7	6	6	6	6	4	6	0	1	0	2012-11-19 13:00:00	2012-11-19 13:09:00	3	\N	\N	\N	\N	\N	timeseries	discrete	quantity	3	0	1
7	8	7	7	7	7	4	7	0	1	0	2012-11-19 13:00:00	2012-11-19 13:09:00	4	\N	\N	\N	\N	\N	timeseries	discrete	quantity	3	0	1
8	9	8	8	8	8	4	8	0	1	0	2012-11-19 13:00:00	2012-11-19 13:49:59	5	\N	\N	\N	\N	\N	timeseries	discrete	quantity	3	1	1
9	10	9	9	9	9	8	9	0	1	0	2008-10-29 00:00:00	2008-10-29 00:00:00	\N	\N	\N	\N	\N	\N	individualObservation	discrete	text	\N	0	1
10	11	9	9	9	9	8	9	0	0	0	2008-10-29 00:00:00	2008-10-29 00:00:00	\N	\N	\N	\N	\N	\N	individualObservation	discrete	text	\N	0	1
11	12	9	9	9	9	8	9	0	1	0	2008-10-29 00:00:00	2008-10-29 00:00:00	\N	\N	\N	\N	\N	\N	individualObservation	discrete	text	\N	0	1
12	13	9	9	9	9	8	9	0	1	0	2012-12-31 23:00:00	2012-12-31 23:00:00	\N	\N	\N	\N	\N	\N	individualObservation	discrete	text	\N	0	1
13	9	8	8	10	10	4	8	0	1	0	2012-11-19 13:00:00	2012-11-19 13:49:59	5	\N	\N	\N	\N	\N	timeseries	discrete	quantity	3	1	1
\.



COPY dataset_reference (fk_dataset_id_from, sort_order, fk_dataset_id_to) FROM stdin;
8	0	13
\.



COPY observation (observation_id, fk_dataset_id, sampling_time_start, sampling_time_end, result_time, identifier, name, description, is_deleted, valid_time_start, valid_time_end, sampling_geometry, value_count, value_boolean, value_category, value_quantity, value_text, value_type, vertical_from, vertical_to, fk_parent_observation_id) FROM stdin;
1	1	2012-11-19 13:00:00	2012-11-19 13:00:00	2012-11-19 13:00:00	http://www.52north.org/test/profile-observation/1	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	\N	profile	10	20	\N
10	1	2012-11-19 13:09:00	2012-11-19 13:09:00	2012-11-19 13:09:00	http://www.52north.org/test/profile-observation/10	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	\N	profile	10	20	\N
11	2	2012-11-19 13:00:00	2012-11-19 13:00:00	2012-11-19 13:00:00	\N	\N	\N	0	\N	\N	\N	0	\N	\N	\N	\N	count	-99999.00	-99999.00	\N
12	2	2012-11-19 13:01:00	2012-11-19 13:01:00	2012-11-19 13:01:00	\N	\N	\N	0	\N	\N	\N	1	\N	\N	\N	\N	count	-99999.00	-99999.00	\N
13	2	2012-11-19 13:02:00	2012-11-19 13:02:00	2012-11-19 13:02:00	\N	\N	\N	0	\N	\N	\N	2	\N	\N	\N	\N	count	-99999.00	-99999.00	\N
14	2	2012-11-19 13:03:00	2012-11-19 13:03:00	2012-11-19 13:03:00	\N	\N	\N	0	\N	\N	\N	3	\N	\N	\N	\N	count	-99999.00	-99999.00	\N
15	2	2012-11-19 13:04:00	2012-11-19 13:04:00	2012-11-19 13:04:00	\N	\N	\N	0	\N	\N	\N	4	\N	\N	\N	\N	count	-99999.00	-99999.00	\N
16	2	2012-11-19 13:05:00	2012-11-19 13:05:00	2012-11-19 13:05:00	\N	\N	\N	0	\N	\N	\N	5	\N	\N	\N	\N	count	-99999.00	-99999.00	\N
17	2	2012-11-19 13:06:00	2012-11-19 13:06:00	2012-11-19 13:06:00	\N	\N	\N	0	\N	\N	\N	6	\N	\N	\N	\N	count	-99999.00	-99999.00	\N
18	2	2012-11-19 13:07:00	2012-11-19 13:07:00	2012-11-19 13:07:00	\N	\N	\N	0	\N	\N	\N	7	\N	\N	\N	\N	count	-99999.00	-99999.00	\N
19	2	2012-11-19 13:08:00	2012-11-19 13:08:00	2012-11-19 13:08:00	\N	\N	\N	0	\N	\N	\N	8	\N	\N	\N	\N	count	-99999.00	-99999.00	\N
20	2	2012-11-19 13:09:00	2012-11-19 13:09:00	2012-11-19 13:09:00	\N	\N	\N	0	\N	\N	\N	9	\N	\N	\N	\N	count	-99999.00	-99999.00	\N
21	3	2012-11-19 13:00:00	2012-11-19 13:00:00	2012-11-19 13:00:00	\N	\N	\N	0	\N	\N	\N	\N	1	\N	\N	\N	boolean	-99999.00	-99999.00	\N
22	3	2012-11-19 13:01:00	2012-11-19 13:01:00	2012-11-19 13:01:00	\N	\N	\N	0	\N	\N	\N	\N	1	\N	\N	\N	boolean	-99999.00	-99999.00	\N
23	3	2012-11-19 13:02:00	2012-11-19 13:02:00	2012-11-19 13:02:00	\N	\N	\N	0	\N	\N	\N	\N	0	\N	\N	\N	boolean	-99999.00	-99999.00	\N
24	3	2012-11-19 13:03:00	2012-11-19 13:03:00	2012-11-19 13:03:00	\N	\N	\N	0	\N	\N	\N	\N	1	\N	\N	\N	boolean	-99999.00	-99999.00	\N
25	3	2012-11-19 13:04:00	2012-11-19 13:04:00	2012-11-19 13:04:00	\N	\N	\N	0	\N	\N	\N	\N	0	\N	\N	\N	boolean	-99999.00	-99999.00	\N
26	3	2012-11-19 13:05:00	2012-11-19 13:05:00	2012-11-19 13:05:00	\N	\N	\N	0	\N	\N	\N	\N	0	\N	\N	\N	boolean	-99999.00	-99999.00	\N
27	3	2012-11-19 13:06:00	2012-11-19 13:06:00	2012-11-19 13:06:00	\N	\N	\N	0	\N	\N	\N	\N	1	\N	\N	\N	boolean	-99999.00	-99999.00	\N
28	3	2012-11-19 13:07:00	2012-11-19 13:07:00	2012-11-19 13:07:00	\N	\N	\N	0	\N	\N	\N	\N	1	\N	\N	\N	boolean	-99999.00	-99999.00	\N
29	3	2012-11-19 13:08:00	2012-11-19 13:08:00	2012-11-19 13:08:00	\N	\N	\N	0	\N	\N	\N	\N	0	\N	\N	\N	boolean	-99999.00	-99999.00	\N
30	3	2012-11-19 13:09:00	2012-11-19 13:09:00	2012-11-19 13:09:00	\N	\N	\N	0	\N	\N	\N	\N	1	\N	\N	\N	boolean	-99999.00	-99999.00	\N
31	4	2012-11-19 13:00:00	2012-11-19 13:00:00	2012-11-19 13:00:00	\N	\N	\N	0	\N	\N	\N	\N	\N	test_category_0	\N	\N	category	-99999.00	-99999.00	\N
32	4	2012-11-19 13:01:00	2012-11-19 13:01:00	2012-11-19 13:01:00	\N	\N	\N	0	\N	\N	\N	\N	\N	test_category_1	\N	\N	category	-99999.00	-99999.00	\N
33	4	2012-11-19 13:02:00	2012-11-19 13:02:00	2012-11-19 13:02:00	\N	\N	\N	0	\N	\N	\N	\N	\N	test_category_2	\N	\N	category	-99999.00	-99999.00	\N
34	4	2012-11-19 13:03:00	2012-11-19 13:03:00	2012-11-19 13:03:00	\N	\N	\N	0	\N	\N	\N	\N	\N	test_category_3	\N	\N	category	-99999.00	-99999.00	\N
35	4	2012-11-19 13:04:00	2012-11-19 13:04:00	2012-11-19 13:04:00	\N	\N	\N	0	\N	\N	\N	\N	\N	test_category_4	\N	\N	category	-99999.00	-99999.00	\N
36	4	2012-11-19 13:05:00	2012-11-19 13:05:00	2012-11-19 13:05:00	\N	\N	\N	0	\N	\N	\N	\N	\N	test_category_5	\N	\N	category	-99999.00	-99999.00	\N
37	4	2012-11-19 13:06:00	2012-11-19 13:06:00	2012-11-19 13:06:00	\N	\N	\N	0	\N	\N	\N	\N	\N	test_category_6	\N	\N	category	-99999.00	-99999.00	\N
38	4	2012-11-19 13:07:00	2012-11-19 13:07:00	2012-11-19 13:07:00	\N	\N	\N	0	\N	\N	\N	\N	\N	test_category_7	\N	\N	category	-99999.00	-99999.00	\N
39	4	2012-11-19 13:08:00	2012-11-19 13:08:00	2012-11-19 13:08:00	\N	\N	\N	0	\N	\N	\N	\N	\N	test_category_8	\N	\N	category	-99999.00	-99999.00	\N
40	4	2012-11-19 13:09:00	2012-11-19 13:09:00	2012-11-19 13:09:00	\N	\N	\N	0	\N	\N	\N	\N	\N	test_category_9	\N	\N	category	-99999.00	-99999.00	\N
41	5	2012-11-19 13:00:00	2012-11-19 13:00:00	2012-11-19 13:00:00	\N	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	test_text_0	text	-99999.00	-99999.00	\N
42	5	2012-11-19 13:01:00	2012-11-19 13:01:00	2012-11-19 13:01:00	\N	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	test_text_1	text	-99999.00	-99999.00	\N
43	5	2012-11-19 13:02:00	2012-11-19 13:02:00	2012-11-19 13:02:00	\N	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	test_text_2	text	-99999.00	-99999.00	\N
44	5	2012-11-19 13:03:00	2012-11-19 13:03:00	2012-11-19 13:03:00	\N	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	test_text_3	text	-99999.00	-99999.00	\N
45	5	2012-11-19 13:04:00	2012-11-19 13:04:00	2012-11-19 13:04:00	\N	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	test_text_4	text	-99999.00	-99999.00	\N
46	5	2012-11-19 13:05:00	2012-11-19 13:05:00	2012-11-19 13:05:00	\N	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	test_text_5	text	-99999.00	-99999.00	\N
47	5	2012-11-19 13:06:00	2012-11-19 13:06:00	2012-11-19 13:06:00	\N	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	test_text_6	text	-99999.00	-99999.00	\N
48	5	2012-11-19 13:07:00	2012-11-19 13:07:00	2012-11-19 13:07:00	\N	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	test_text_7	text	-99999.00	-99999.00	\N
49	5	2012-11-19 13:08:00	2012-11-19 13:08:00	2012-11-19 13:08:00	\N	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	test_text_8	text	-99999.00	-99999.00	\N
50	5	2012-11-19 13:09:00	2012-11-19 13:09:00	2012-11-19 13:09:00	\N	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	test_text_9	text	-99999.00	-99999.00	\N
51	6	2012-11-19 13:00:00	2012-11-19 13:00:00	2012-11-19 13:00:00	\N	\N	\N	0	\N	\N	\N	\N	\N	\N	2.0	\N	quantity	-99999.00	-99999.00	\N
52	6	2012-11-19 13:01:00	2012-11-19 13:01:00	2012-11-19 13:01:00	\N	\N	\N	0	\N	\N	\N	\N	\N	\N	2.1	\N	quantity	-99999.00	-99999.00	\N
53	6	2012-11-19 13:02:00	2012-11-19 13:02:00	2012-11-19 13:02:00	\N	\N	\N	0	\N	\N	\N	\N	\N	\N	2.2	\N	quantity	-99999.00	-99999.00	\N
54	6	2012-11-19 13:03:00	2012-11-19 13:03:00	2012-11-19 13:03:00	\N	\N	\N	0	\N	\N	\N	\N	\N	\N	2.3	\N	quantity	-99999.00	-99999.00	\N
55	6	2012-11-19 13:04:00	2012-11-19 13:04:00	2012-11-19 13:04:00	\N	\N	\N	0	\N	\N	\N	\N	\N	\N	2.4	\N	quantity	-99999.00	-99999.00	\N
56	6	2012-11-19 13:05:00	2012-11-19 13:05:00	2012-11-19 13:05:00	\N	\N	\N	0	\N	\N	\N	\N	\N	\N	2.5	\N	quantity	-99999.00	-99999.00	\N
57	6	2012-11-19 13:06:00	2012-11-19 13:06:00	2012-11-19 13:06:00	\N	\N	\N	0	\N	\N	\N	\N	\N	\N	2.6	\N	quantity	-99999.00	-99999.00	\N
58	6	2012-11-19 13:07:00	2012-11-19 13:07:00	2012-11-19 13:07:00	\N	\N	\N	0	\N	\N	\N	\N	\N	\N	2.7	\N	quantity	-99999.00	-99999.00	\N
59	6	2012-11-19 13:08:00	2012-11-19 13:08:00	2012-11-19 13:08:00	\N	\N	\N	0	\N	\N	\N	\N	\N	\N	2.8	\N	quantity	-99999.00	-99999.00	\N
60	6	2012-11-19 13:09:00	2012-11-19 13:09:00	2012-11-19 13:09:00	\N	\N	\N	0	\N	\N	\N	\N	\N	\N	2.9	\N	quantity	-99999.00	-99999.00	\N
61	7	2012-11-19 13:00:00	2012-11-19 13:00:00	2012-11-19 13:00:00	\N	\N	\N	0	\N	\N	\N	\N	\N	\N	3.0	\N	quantity	-99999.00	-99999.00	\N
62	7	2012-11-19 13:01:00	2012-11-19 13:01:00	2012-11-19 13:01:00	\N	\N	\N	0	\N	\N	\N	\N	\N	\N	3.1	\N	quantity	-99999.00	-99999.00	\N
63	7	2012-11-19 13:02:00	2012-11-19 13:02:00	2012-11-19 13:02:00	\N	\N	\N	0	\N	\N	\N	\N	\N	\N	3.2	\N	quantity	-99999.00	-99999.00	\N
64	7	2012-11-19 13:03:00	2012-11-19 13:03:00	2012-11-19 13:03:00	\N	\N	\N	0	\N	\N	\N	\N	\N	\N	3.3	\N	quantity	-99999.00	-99999.00	\N
65	7	2012-11-19 13:04:00	2012-11-19 13:04:00	2012-11-19 13:04:00	\N	\N	\N	0	\N	\N	\N	\N	\N	\N	3.4	\N	quantity	-99999.00	-99999.00	\N
66	7	2012-11-19 13:05:00	2012-11-19 13:05:00	2012-11-19 13:05:00	\N	\N	\N	0	\N	\N	\N	\N	\N	\N	3.5	\N	quantity	-99999.00	-99999.00	\N
67	7	2012-11-19 13:06:00	2012-11-19 13:06:00	2012-11-19 13:06:00	\N	\N	\N	0	\N	\N	\N	\N	\N	\N	3.6	\N	quantity	-99999.00	-99999.00	\N
68	7	2012-11-19 13:07:00	2012-11-19 13:07:00	2012-11-19 13:07:00	\N	\N	\N	0	\N	\N	\N	\N	\N	\N	3.7	\N	quantity	-99999.00	-99999.00	\N
69	7	2012-11-19 13:08:00	2012-11-19 13:08:00	2012-11-19 13:08:00	\N	\N	\N	0	\N	\N	\N	\N	\N	\N	3.8	\N	quantity	-99999.00	-99999.00	\N
70	7	2012-11-19 13:09:00	2012-11-19 13:09:00	2012-11-19 13:09:00	\N	\N	\N	0	\N	\N	\N	\N	\N	\N	3.9	\N	quantity	-99999.00	-99999.00	\N
71	8	2012-11-19 13:00:00	2012-11-19 13:04:59	2012-11-18 13:00:00	http://www.52north.org/test/observation/8_71_resulttime_2012-11-18	\N	\N	0	\N	\N	0101000020E61000000000000000001C400000000000004940	\N	\N	\N	4.0	\N	quantity	-99999.00	-99999.00	\N
72	8	2012-11-19 13:05:00	2012-11-19 13:09:59	2012-11-18 13:00:00	http://www.52north.org/test/observation/8_72_resulttime_2012-11-18	\N	\N	0	\N	\N	0101000020E6100000CDCCCCCCCCCC1C40CDCCCCCCCC4C4940	\N	\N	\N	4.1	\N	quantity	-99999.00	-99999.00	\N
73	8	2012-11-19 13:10:00	2012-11-19 13:14:59	2012-11-18 13:00:00	http://www.52north.org/test/observation/8_73_resulttime_2012-11-18	\N	\N	0	\N	\N	0101000020E61000003333333333331F40CDCCCCCCCCCC4940	\N	\N	\N	4.2	\N	quantity	-99999.00	-99999.00	\N
74	8	2012-11-19 13:15:00	2012-11-19 13:19:59	2012-11-18 13:00:00	http://www.52north.org/test/observation/8_74_resulttime_2012-11-18	\N	\N	0	\N	\N	0101000020E6100000CDCCCCCCCCCC20409A99999999994940	\N	\N	\N	4.3	\N	quantity	-99999.00	-99999.00	\N
75	8	2012-11-19 13:20:00	2012-11-19 13:24:59	2012-11-18 13:00:00	http://www.52north.org/test/observation/8_75_resulttime_2012-11-18	\N	\N	0	\N	\N	0101000020E61000009A99999999991D409A99999999194940	\N	\N	\N	4.4	\N	quantity	-99999.00	-99999.00	\N
76	8	2012-11-19 13:25:00	2012-11-19 13:29:59	2012-11-18 13:00:00	http://www.52north.org/test/observation/8_76_resulttime_2012-11-18	\N	\N	0	\N	\N	0101000020E61000006666666666661E406666666666664940	\N	\N	\N	4.5	\N	quantity	-99999.00	-99999.00	\N
77	8	2012-11-19 13:30:00	2012-11-19 13:34:59	2012-11-18 13:00:00	http://www.52north.org/test/observation/8_77_resulttime_2012-11-18	\N	\N	0	\N	\N	0101000020E610000000000000000020406666666666664940	\N	\N	\N	4.6	\N	quantity	-99999.00	-99999.00	\N
78	8	2012-11-19 13:35:00	2012-11-19 13:39:59	2012-11-18 13:00:00	http://www.52north.org/test/observation/8_78_resulttime_2012-11-18	\N	\N	0	\N	\N	0101000020E610000000000000000021406666666666664940	\N	\N	\N	4.7	\N	quantity	-99999.00	-99999.00	\N
79	8	2012-11-19 13:40:00	2012-11-19 13:44:59	2012-11-18 13:00:00	http://www.52north.org/test/observation/8_79_resulttime_2012-11-18	\N	\N	0	\N	\N	0101000020E610000033333333333320409A99999999194940	\N	\N	\N	4.8	\N	quantity	-99999.00	-99999.00	\N
80	8	2012-11-19 13:45:00	2012-11-19 13:49:59	2012-11-18 13:00:00	http://www.52north.org/test/observation/8_80_resulttime_2012-11-18	\N	\N	0	\N	\N	0101000020E610000033333333333321400000000000404940	\N	\N	\N	4.9	\N	quantity	-99999.00	-99999.00	\N
81	9	2008-10-29 00:00:00	2008-10-29 00:00:00	2008-10-29 00:00:00	\N	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	Carsten Hollmann	text	-99999.00	-99999.00	\N
82	10	2008-10-29 00:00:00	2008-10-29 00:00:00	2008-10-29 00:00:00	\N	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	Christian Autermann	text	-99999.00	-99999.00	\N
83	11	2008-10-29 00:00:00	2008-10-29 00:00:00	2008-10-29 00:00:00	\N	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	Shane StClair	text	-99999.00	-99999.00	\N
84	12	2012-12-31 23:00:00	2012-12-31 23:00:00	2012-12-31 22:01:00	\N	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	John/Jane Doe	text	-99999.00	-99999.00	\N
85	1	2012-11-19 13:00:00	2012-11-19 13:00:00	2012-11-19 13:00:00	http://www.52north.org/test/observation/1_firstValue_vertical_0	\N	\N	0	\N	\N	\N	\N	\N	\N	100	\N	quantity	-99999.00	10	1
86	1	2012-11-19 13:00:00	2012-11-19 13:00:00	2012-11-19 13:00:00	http://www.52north.org/test/observation/1_firstValue_vertical_1	\N	\N	0	\N	\N	\N	\N	\N	\N	200	\N	quantity	-99999.00	20	1
87	1	2012-11-19 13:09:00	2012-11-19 13:09:00	2012-11-19 13:09:00	http://www.52north.org/test/observation/1_lastValue_vertical_0	\N	\N	0	\N	\N	\N	\N	\N	\N	300	\N	quantity	-9999.00	15	10
88	1	2012-11-19 13:09:00	2012-11-19 13:09:00	2012-11-19 13:09:00	http://www.52north.org/test/observation/1_lastValue_vertical_1	\N	\N	0	\N	\N	\N	\N	\N	\N	400	\N	quantity	-9999.00	12	10
91	8	2012-11-19 13:00:00	2012-11-19 13:04:59	2012-11-19 13:00:00	http://www.52north.org/test/observation/8_91_resulttime_2012-11-19	\N	\N	0	\N	\N	0101000020E610000000000000000024400000000000002440	\N	\N	\N	5.0	\N	quantity	-9999.00	-9999.00	\N
92	8	2012-11-19 13:05:00	2012-11-19 13:09:59	2012-11-19 13:00:00	http://www.52north.org/test/observation/8_92_resulttime_2012-11-19	\N	\N	0	\N	\N	0101000020E6100000CDCCCCCCCCCC1C40CDCCCCCCCC4C4940	\N	\N	\N	5.1	\N	quantity	-9999.00	-9999.00	\N
93	8	2012-11-19 13:10:00	2012-11-19 13:14:59	2012-11-19 13:00:00	http://www.52north.org/test/observation/8_93_resulttime_2012-11-19	\N	\N	0	\N	\N	0101000020E61000003333333333331F40CDCCCCCCCCCC4940	\N	\N	\N	5.2	\N	quantity	-9999.00	-9999.00	\N
94	8	2012-11-19 13:15:00	2012-11-19 13:19:59	2012-11-19 13:00:00	http://www.52north.org/test/observation/8_94_resulttime_2012-11-19	\N	\N	0	\N	\N	0101000020E6100000CDCCCCCCCCCC20409A99999999994940	\N	\N	\N	5.3	\N	quantity	-9999.00	-9999.00	\N
95	8	2012-11-19 13:20:00	2012-11-19 13:24:59	2012-11-19 13:00:00	http://www.52north.org/test/observation/8_95_resulttime_2012-11-19	\N	\N	0	\N	\N	0101000020E61000009A99999999991D409A99999999194940	\N	\N	\N	5.4	\N	quantity	-9999.00	-9999.00	\N
96	8	2012-11-19 13:25:00	2012-11-19 13:29:59	2012-11-19 13:00:00	http://www.52north.org/test/observation/8_96_resulttime_2012-11-19	\N	\N	0	\N	\N	0101000020E61000006666666666661E406666666666664940	\N	\N	\N	5.5	\N	quantity	-9999.00	-9999.00	\N
97	8	2012-11-19 13:30:00	2012-11-19 13:34:59	2012-11-19 13:00:00	http://www.52north.org/test/observation/8_97_resulttime_2012-11-19	\N	\N	0	\N	\N	0101000020E610000000000000000020406666666666664940	\N	\N	\N	5.6	\N	quantity	-9999.00	-9999.00	\N
98	8	2012-11-19 13:35:00	2012-11-19 13:39:59	2012-11-19 13:00:00	http://www.52north.org/test/observation/8_98_resulttime_2012-11-19	\N	\N	0	\N	\N	0101000020E610000000000000000021406666666666664940	\N	\N	\N	5.7	\N	quantity	-9999.00	-9999.00	\N
99	8	2012-11-19 13:40:00	2012-11-19 13:44:59	2012-11-19 13:00:00	http://www.52north.org/test/observation/8_99_resulttime_2012-11-19	\N	\N	0	\N	\N	0101000020E610000033333333333320409A99999999194940	\N	\N	\N	5.8	\N	quantity	-9999.00	-9999.00	\N
100	8	2012-11-19 13:45:00	2012-11-19 13:49:59	2012-11-19 13:00:00	http://www.52north.org/test/observation/8_100_resulttime_2012-11-19	\N	\N	0	\N	\N	0101000020E61000003333333333331F400000000000804940	\N	\N	\N	5.9	\N	quantity	-9999.00	-9999.00	\N
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
update dataset set fk_first_observation_id=71,fk_last_observation_id=100,first_value=4.0,last_value=5.9 where dataset_id=13;


SELECT pg_catalog.setval('feature_seq', 13, true);


SELECT pg_catalog.setval('category_seq', 9, true);


SELECT pg_catalog.setval('phenomenon_seq', 9, true);



SELECT pg_catalog.setval('observation_seq', 100, true);



SELECT pg_catalog.setval('offering_seq', 9, true);



SELECT pg_catalog.setval('procedure_seq', 9, true);



COPY value_profile (fk_observation_id, vertical_from_name, vertical_to_name, fk_vertical_unit_id) FROM stdin;
1	depth	\N	6
10	depth	\N	6
\.


SELECT pg_catalog.setval('dataset_seq', 12, true);



SELECT pg_catalog.setval('unit_seq', 5, true);



COPY measuring_program (measuring_program_id, identifier, name, description, producer, measuring_time_start, measuring_time_end) FROM stdin;
1	52N	52North	Messprogramm von 52N	52N	2017-03-04 13:49:27.394	2018-03-04 13:49:27.394
2	0815	nix acht fuenfzehn	0815 Messprogramm	unknown	2011-11-04 13:49:27.394	\N
3	WV4711	Wupperverband	Messprogramm des Wupperverbands	Wupperverband	2018-03-04 13:49:27.394	2018-11-04 13:49:27.394
\.



SELECT pg_catalog.setval('measuring_program_seq', 3, true);



COPY measuring_program_dataset (fk_dataset_id, fk_measuring_program_id) FROM stdin;
1	1
2	2
3	3
6	1
7	1
4	3
5	3
8	3
\.



COPY sampling (sampling_id, fk_measuring_program_id, identifier, name, description, sampler, sampling_method, environmental_conditions, sampling_time_start, sampling_time_end) FROM stdin;
1	1	52N-9654235	\N	\N	52Nler	Messbecher	heiter	2017-06-04 13:49:27.394	2017-06-04 13:59:27.394
2	1	52N955	\N	messung erfolgt	52Nler	Messbecher	bedeckt	2017-12-04 13:49:27.394	2017-12-04 13:50:27.394
3	2	0815/66546	\N	keine Vorkommnisse	niemand	Bodenprobe	\N	2017-03-04 13:49:27.394	2017-03-04 13:49:27.394
4	3	WV/6956	\N	erledigt	WVler	automatisch	heiter	2018-03-04 13:49:27.394	2018-03-05 13:49:27.394
5	3	WV-8733	\N	Regen	WVler 27	automatisch	wolkig	2018-03-05 13:49:27.394	2018-03-05 13:49:27.394
6	3	WV-AF889	\N	\N	WVler	automatisch	nebel	2018-03-05 13:49:27.394	2018-03-05 13:49:27.394
\.



COPY sampling_dataset (fk_dataset_id, fk_sampling_id) FROM stdin;
1	2
2	3
3	4
6	1
7	2
4	5
5	6
8	4
\.


-- Completed on 2017-07-18 15:11:04 CEST

--
-- PostgreSQL database dump complete
--

