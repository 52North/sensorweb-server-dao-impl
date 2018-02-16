## Test REST API endpoint with test data
After inserting the test data, we can use Postman or Newman to run the tests. 

### Data Preparation
Start the webapp with property `series.hibernate.hbm2ddl.auto=create-drop`

Insert test data into a fresh SOS database:
```
$ sudo su postgres -c "psql -d test_sos_db < tests/example-data.sql"
```

### Running the tests

For doing this on command line:
```
newman run tests/sos-example-data.postman_collection.json -e tests/localhost_8080_.postman_environment.json -r cli
```
