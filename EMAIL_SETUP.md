# Email Notification Setup for Neon Survivor CI

The CI workflow now emails you a download link after every successful build!

## Quick Setup (5 minutes)

### Step 1: Create Gmail App Password

1. Go to your Google Account: https://myaccount.google.com/
2. Navigate to **Security** → **2-Step Verification** (enable if not already)
3. Scroll to **App passwords** → Click it
4. Create new app password:
   - App: "Mail"
   - Device: "GitHub Actions"
5. **Copy the 16-character password** (you'll need this!)

### Step 2: Add GitHub Secrets

1. Go to your repo: https://github.com/timmseth/survivalClicker
2. Click **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret** and add these 3 secrets:

#### Secret 1: `MAIL_USERNAME`
- Name: `MAIL_USERNAME`
- Value: `timmons.seth@gmail.com` (your Gmail address)

#### Secret 2: `MAIL_PASSWORD`
- Name: `MAIL_PASSWORD`
- Value: `xxxx xxxx xxxx xxxx` (the 16-char app password from Step 1)

#### Secret 3: `MAIL_TO`
- Name: `MAIL_TO`
- Value: `timmons.seth@gmail.com` (or multiple emails separated by commas)

**For multiple recipients:**
```
timmons.seth@gmail.com, friend@example.com, tester@example.com
```

### Step 3: Done!

Next time you push, you'll get an email with:
- 📦 Direct link to download APK
- 📋 Commit info
- 🔗 Link to build logs
- 🎮 One-click artifact download!

---

## Email Format Preview

**Subject:** 🎮 Neon Survivor Build #42 Ready!

**Body:**
```
Build #42 completed successfully!

📦 Download APK:
https://github.com/timmseth/survivalClicker/actions/runs/...

📝 Commit: abc1234...
💬 Message: Update README with comprehensive documentation
🌿 Branch: refs/heads/main

Click "neon-survivor-apk" artifact to download the APK!
```

No attachment needed - just click the link to download from GitHub Actions!

---

## Troubleshooting

### Email not sending?
- Check that secrets are named **exactly**: `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_TO`
- Verify Gmail app password is correct (16 characters, no spaces)
- Check Actions logs: https://github.com/timmseth/survivalClicker/actions

### Want to use a different email provider?
Change `server_address` and `server_port` in `.github/workflows/android.yml`:
- **Gmail**: `smtp.gmail.com:465`
- **Outlook**: `smtp-mail.outlook.com:587`
- **Yahoo**: `smtp.mail.yahoo.com:465`

### Want to disable email?
Comment out or remove the "Send APK via Email" step in the workflow file.

---

## Security Notes

✅ **Safe**: App passwords are separate from your main Gmail password
✅ **Revokable**: Delete app password anytime from Google Account settings
✅ **Encrypted**: GitHub secrets are encrypted and only visible to Actions
❌ **Never commit**: Don't put passwords directly in the workflow file!
