# gcp

An example `AssetProvider` implementation for Google Cloud Services.

## Cloud Storage
To use Google Cloud Storage, use the same uri you normally would with Google's `gsutil`:

```properties
dbms.container.ssl.policy.https.private_key=gs://mah-bucket/mykey.pem
```

**Known Issues**
- assumes you're running in GCP or providing the credentials via environment variables...no mechanisms are in place to override this
- logs to stdout like a loser

## Secrets Manager
**TODO**