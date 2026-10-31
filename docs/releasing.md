# Release secrets

The release workflow needs four repository secrets: three for signing the plugin and one for publishing it to JetBrains Marketplace.

| Secret | What it holds |
|---|---|
| `CERTIFICATE_CHAIN` | The certificate, `chain.crt` |
| `PRIVATE_KEY` | The private key, `private.pem` |
| `PRIVATE_KEY_PASSWORD` | The key's password |
| `PUBLISH_TOKEN` | A JetBrains Marketplace token |

## 1. Create the signing key and certificate

Run these on your Mac, in a folder outside the repo, for example `~/simpanel-signing`.

Create a private key encrypted with a password. It asks for the password twice, and that password becomes `PRIVATE_KEY_PASSWORD`:

```sh
openssl genpkey -aes-256-cbc -algorithm RSA -out private_encrypted.pem -pkeyopt rsa_keygen_bits:4096
```

Write out an unencrypted copy of the key, which [JetBrains' guide](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html) uses for `PRIVATE_KEY`. It asks for the password from the last step:

```sh
openssl rsa -in private_encrypted.pem -out private.pem
```

Create a self-signed certificate from the key. It asks for a name, country and so on; only "Common Name" matters, so put your name there:

```sh
openssl req -key private.pem -new -x509 -days 3650 -out chain.crt
```

`-days 3650` makes the certificate valid for 10 years. JetBrains' example uses 365, which means renewing every year.

## 2. Create the Marketplace token

1. Sign in at [plugins.jetbrains.com](https://plugins.jetbrains.com).
2. Click your avatar, then **Profile** → **My Tokens**.
3. Click **Generate Token**, name it something like `simpanel-ci`, and copy it. It's shown only once.

## 3. Add the four secrets to GitHub

From the repo folder, with the `gh` CLI signed in:

```sh
gh secret set CERTIFICATE_CHAIN < ~/simpanel-signing/chain.crt
gh secret set PRIVATE_KEY < ~/simpanel-signing/private.pem
gh secret set PRIVATE_KEY_PASSWORD
gh secret set PUBLISH_TOKEN
```

The last two prompt you to paste the value: your key password and the Marketplace token.

You can also add them on the web, under the repo's **Settings** → **Secrets and variables** → **Actions** → **New repository secret**. Paste each file's whole contents, including the `-----BEGIN …-----` and `-----END …-----` lines.

## 4. Upload the first version by hand

Marketplace won't let CI create a new plugin, so the first version has to go up through the website:

```sh
cd plugin && ./gradlew buildPlugin
```

Then, on plugins.jetbrains.com, open your profile → **Add new plugin** and upload `plugin/build/distributions/simpanel-<version>.zip`. After JetBrains approves it, tagged releases publish on their own:

```sh
git tag v0.2.0 && git push origin v0.2.0
```

## 5. Keep the keys safe

- Store `private_encrypted.pem`, `chain.crt` and the password in your password manager. Every future release needs the same key.
- Delete the unencrypted copy after adding the secrets:

  ```sh
  rm ~/simpanel-signing/private.pem
  ```

- Never commit any of these files. `.gitignore` skips `*.pem` and `*.crt` in case they land in the repo folder, but keep them outside it.
