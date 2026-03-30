# Railway Deployment Guide - Silent Hall UMAT Quiz System

## Prerequisites
- Railway.app account (https://railway.app)
- Git repository connected to Railway
- MySQL/PostgreSQL database (Railway provides this)

## Step 1: Connect Repository to Railway

1. Go to [Railway Dashboard](https://railway.app)
2. Click **"New Project"** → **"GitHub"**
3. Select your repository (SILENT-HALL)
4. Railway will auto-detect the Dockerfile and deploy

## Step 2: Configure Database Plugin

1. In Railway Dashboard, go to your project
2. Click **"New"** → **"MySQL"** (or PostgreSQL)
3. Note the auto-generated credentials:
   ```
   DATABASE_URL
   MYSQL_USER
   MYSQL_PASSWORD
   MYSQL_DB
   ```

## Step 3: Set Environment Variables

Add these to your Railway project settings:

```
JWT_SECRET=UMATQuizSystemSecretKey2026VeryLongAndSecureKeyForJWT
SPRING_PROFILES_ACTIVE=railway
PORT=8080

# Database (Railway auto-provides these)
DATABASE_URL=mysql://user:password@hostname:port/database
DATABASE_USER=user
DATABASE_PASSWORD=password
DATABASE_DRIVER=com.mysql.cj.jdbc.Driver
```

## Step 4: Update Frontend API URL

Your frontend will automatically use the Railway backend URL if deployed together. The API endpoint is:
```
https://your-project.up.railway.app/api
```

Edit `frontend/js/umat-api-railway.js` and integrate it into your HTML files.

## Step 5: Deploy

1. Push changes to GitHub:
   ```bash
   git add .
   git commit -m "Add Railway deployment configuration"
   git push origin main
   ```

2. Railway will automatically rebuild and redeploy

## Accessing the Application

- **Frontend**: `https://your-project.up.railway.app`
- **Backend API**: `https://your-project.up.railway.app/api`
- **Login**: Browse to frontend URL

## Troubleshooting

### Port Already in Use
Railway automatically assigns ports, no manual configuration needed.

### Database Connection Failed
- Check DATABASE_URL environment variable
- Ensure MySQL plugin is connected
- Verify credentials in Railway dashboard

### Application Won't Start
- Check build logs in Railway dashboard
- Verify JAR file built successfully
- Check Docker logs

## File Structure for Deployment

```
SILENT-HALL/
├── Dockerfile              # Docker build configuration
├── Procfile               # Process configuration
├── railway.json           # Railway configuration
├── .env.example           # Environment variables template
├── backend/
│   ├── pom.xml
│   ├── src/
│   └── target/quiz-system-1.0.0.jar  # Built JAR
└── frontend/
    ├── js/umat-api-railway.js
    └── ... (static HTML/CSS)
```

## Cost Optimization

Railway offers:
- **Free tier**: $5/month credit
- MySQL database: ~$5/month
- Backend: ~$5-10/month depending on traffic

Total estimated cost: **$10-15/month**

## Setting Up Automatic Deployments

1. Enable GitHub integration in Railway
2. Choose deployment branch (main/master)
3. Railway automatically deploys on push
4. Monitor builds in Railway dashboard

## Useful Commands

```bash
# View Railway logs
railway logs

# Connect to Railway shell
railway shell

# Check environment variables
railway env

# Redeploy current version
railway deploy
```

## Additional Resources

- [Railway Documentation](https://docs.railway.app)
- [Spring Boot on Railway](https://docs.railway.app/guides/deployment)
- [MySQL Plugin Guide](https://docs.railway.app/guides/mysql)
