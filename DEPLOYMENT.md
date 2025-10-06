# Tire Management System - Deployment Guide 🚀

## මේ project එක host කරන්න විදිහ

### 1. Heroku (Recommended - Free/Paid)

#### Setup Steps:
1. Heroku account එකක් හදාගන්න: https://heroku.com
2. Heroku CLI install කරන්න: https://devcenter.heroku.com/articles/heroku-cli
3. Git repository එකක් setup කරන්න:
   ```bash
   git init
   git add .
   git commit -m "Initial commit"
   ```

4. Heroku app එකක් create කරන්න:
   ```bash
   heroku create your-tire-management-app
   ```

5. MongoDB Atlas connection string set කරන්න:
   ```bash
   heroku config:set MONGODB_URI="mongodb+srv://slthrmanager:P7jMbfeiv8FrxJsY@cluster0.ndvz7mp.mongodb.net/tire_management?retryWrites=true&w=majority&appName=Cluster0"
   ```

6. Deploy කරන්න:
   ```bash
   git push heroku main
   ```

#### Cost: Free tier available (550 hours/month)

---

### 2. Railway (Modern & Easy)

1. Railway account එකක් හදාගන්න: https://railway.app
2. GitHub repository එකක් create කරන්න
3. Railway වලට connect කරන්න
4. Environment variables add කරන්න:
   - `MONGODB_URI`: MongoDB Atlas connection string
   - `PORT`: 8080

#### Cost: $5/month after free trial

---

### 3. Render (Free tier available)

1. Render account එකක් හදාගන්න: https://render.com
2. GitHub repo connect කරන්න
3. Web Service create කරන්න:
   - Build Command: `./mvnw clean package -DskipTests`
   - Start Command: `java -jar target/tire_management-0.0.1-SNAPSHOT.jar`

#### Cost: Free tier available (limited hours)

---

### 4. AWS Elastic Beanstalk

1. AWS account එකක් ඕන
2. JAR file upload කරන්න
3. Environment variables configure කරන්න

#### Cost: Pay as you use

---

### 5. Digital Ocean App Platform

1. Digital Ocean account එකක් ඕන
2. GitHub repo connect කරන්න
3. Java app deploy කරන්න

#### Cost: $5/month minimum

---

## Quick Start - Heroku Deploy කරන්න:

1. Git setup:
```bash
git init
git add .
git commit -m "Ready for deployment"
```

2. Heroku login:
```bash
heroku login
```

3. Create app:
```bash
heroku create my-tire-system
```

4. Set MongoDB:
```bash
heroku config:set MONGODB_URI="mongodb+srv://slthrmanager:P7jMbfeiv8FrxJsY@cluster0.ndvz7mp.mongodb.net/tire_management?retryWrites=true&w=majority&appName=Cluster0"
```

5. Deploy:
```bash
git push heroku main
```

Your app will be available at: `https://my-tire-system.herokuapp.com`

---

## Important Notes:

1. **Environment Variables**: Production වලදී passwords හා sensitive data environment variables වලට දාන්න
2. **CORS**: Frontend URL එක backend වල allow කරන්න
3. **Database**: MongoDB Atlas දැනටමත් cloud වල තියෙනවා
4. **File Storage**: Production වල file uploads වලට cloud storage (AWS S3) use කරන්න
