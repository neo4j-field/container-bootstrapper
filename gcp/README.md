# gcp

An example `AssetProvider` implementation for Google Cloud Services.

## Cloud Storage
To use Google Cloud Storage, use the same uri you normally would with Google's `gsutil`:

```properties
dbms.container.ssl.policy.https.public_certificate=gs://mah-bucket/mykey.pem
```

## Secrets Manager
To use Google Secret Manager, you need the project id, secret name, and secret version:

```properties
dbms.container.ssl.policy.https.private_key=gsm://my-project/my-private-key?version=2
```

The `version` can be left off entirely and it will default to the "latest" version available.

---
**Known Issues**
- assumes you're running in GCP or providing the credentials via environment variables...no mechanisms are in place to override this
- logs to stdout like a loser
