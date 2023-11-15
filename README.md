# employee-wfh-expenses-frontend

## Info

The employee-wfh-expenses-frontend service allows individuals to claim tax relief on expenses for working from home

The service uses TAI to update the user's tax account via IABD 59

### Dependencies

|Service               |Link                                                                    |
|----------------------|------------------------------------------------------------------------|
|Tai                   |https://github.com/hmrc/tai                                             |
|Citizen Details       |https://github.com/hmrc/citizen-details                                 |
|Preferences           |https://github.com/hmrc/preferences-frontend                            |
|EE Eligibility        |https://github.com/hmrc/employment-expenses-tax-relief-guidance-frontend|

### Endpoints used

|Service        |HTTP Method |Route                                  |Purpose |
|---------------|--- |----------------|----------------------------------|
|Tai            |GET |/tai/$nino/tax-account/$year /expenses/employee-expenses/$iabd| Returns details of a users tax account specifically that of IABD 59 |
|Tai            |POST|/tai/$nino/tax-account/$year /expenses/working-from-home-employee-expenses/$iabd| Updates a users tax account specifically that of IABD 59  |
|Citizen Details|GET |/citizen-details/$nino/$etag|Retrieves the users etag which is added to their update request to NPS to ensure optimistic locking|
|Preferences    |GET |/preferences-frontend/paperless|Retrieves the users paperless preference status|

## Running the service locally

##### `run.sh`

* Starts the Play! server on [localhost:9336](http://localhost:9336) with test routes enabled.

### Start dependencies via Service Manager

To start all dependencies and services for Employee Expenses WFH, use one of the following commands:
```
sm --start EE_ALL -r
sm --start EE_DEP -r (starts only dependencies).
```

Navigate to login stub (http://localhost:9949/auth-login-stub/) 

Redirect URL: http://localhost:9336/employee-working-from-home-expenses/

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
