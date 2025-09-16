# pwg
# مهرجان الكرازة المرقسية 2026 - Festival Art Voting System

A comprehensive web application for managing art festivals with voting capabilities, built for the St. Mark's Preaching Festival 2026.

## 🎨 Features

### 🔐 User Authentication
- User registration and login
- Secure password hashing
- Role-based access (Users & Admins)
- Session management

### 🖼️ Artwork Management
- Upload artwork with images, titles, and descriptions
- Categorized organization (Paintings, Photography, Sculptures, etc.)
- Admin approval workflow
- Featured artwork highlighting

### 🗳️ Voting System
- One vote per user per artwork
- Real-time vote counting
- Vote removal/change capability
- Anti-fraud protection

### 📊 Statistics & Analytics
- Live festival statistics
- Top-voted artworks ranking
- Participant engagement metrics
- Real-time updates

### 🛠️ Admin Panel
- Artwork approval/rejection
- Content moderation
- Category management
- Featured content control

## 🚀 Quick Start

### Prerequisites
- Python 3.7+
- pip (Python package manager)

### Installation

1. **Clone the repository**
```bash
git clone <repository-url>
cd festival-art-voting
```

2. **Install dependencies**
```bash
pip install flask flask-sqlalchemy flask-cors werkzeug
```

3. **Run the application**
```bash
python learn.py
```

4. **Access the application**
   - Open your browser and go to: `http://localhost:5000`
   - The database will be automatically created on first run

## 📁 Project Structure

```
festival-art-voting/
├── learn.py              # Main Flask application
├── static/
│   ├── css/
│   │   └── main.css     # Stylesheet with RTL support
│   └── uploads/         # Uploaded artwork files
├── templates/
│   └── index.html       # Main application template
└── festival_art.db     # SQLite database (auto-created)
```

## 🔧 Configuration

### Environment Variables
Edit these values in `learn.py`:
```python
app.config['SECRET_KEY'] = 'your-secret-key-change-this'  # Change this!
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///festival_art.db'
app.config['UPLOAD_FOLDER'] = os.path.join('static', 'uploads')
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024  # 16MB limit
```

### Default Categories
The system automatically creates these categories:
- 🎨 Paintings (Traditional and digital paintings)
- 📸 Photography (Photographic works)
- 🗿 Sculptures (3D artistic works)
- 💻 Digital Art (Computer-generated artwork)
- 🎭 Mixed Media (Art using multiple mediums)

## 📚 API Documentation

### Authentication Endpoints
```
POST /api/auth/register    # User registration
POST /api/auth/login       # User login
POST /api/auth/logout      # User logout
GET  /api/auth/me          # Get current user info
```

### Artwork Endpoints
```
GET  /api/artworks              # Get artworks (with pagination)
GET  /api/artworks/<id>         # Get specific artwork
POST /api/artworks              # Upload new artwork (auth required)
POST /api/artworks/<id>/vote    # Vote for artwork (auth required)
DELETE /api/artworks/<id>/vote  # Remove vote (auth required)
```

### Admin Endpoints
```
GET /api/admin/artworks                    # Get all artworks (admin)
PUT /api/admin/artworks/<id>/approve       # Approve artwork (admin)
PUT /api/admin/artworks/<id>/feature       # Toggle featured status (admin)
```

### Statistics Endpoints
```
GET /api/statistics    # Get festival statistics
GET /api/top-voted     # Get top-voted artworks
GET /api/categories    # Get all categories
```

## 🎯 Usage Guide

### For Artists
1. **Register** an account
2. **Login** to your account
3. Click **"إضافة عمل فني"** (Add Artwork)
4. Fill in artwork details and upload image
5. **Wait** for admin approval
6. Your artwork will appear once approved

### For Visitors
1. **Browse** artworks on the main page
2. **Register/Login** to vote
3. Click **"صوت"** (Vote) on your favorite pieces
4. View **real-time statistics** and rankings

### For Administrators
1. Login with admin account
2. **Approve/reject** submitted artworks
3. **Feature** exceptional works
4. **Manage** categories and content

## 🌐 Localization

This application is designed for Arabic-speaking users:
- **RTL Layout**: Right-to-left text direction
- **Arabic Interface**: All text in Arabic
- **Cultural Adaptation**: Designed for Middle Eastern audiences

## 🔒 Security Features

- **Password Hashing**: Using Werkzeug's security functions
- **File Validation**: Only image files allowed
- **Size Limits**: 16MB maximum file size
- **Session Security**: Secure session management
- **Input Sanitization**: Protection against malicious uploads

## 🐛 Troubleshooting

### Common Issues

**Database not found**
- The database is created automatically on first run
- Ensure write permissions in the project directory

**Upload fails**
- Check file size (max 16MB)
- Ensure file is an image (png, jpg, jpeg, gif, webp)
- Verify upload folder permissions

**Styling issues**
- Clear browser cache
- Check CSS file path in templates

**API connection errors**
- Ensure Flask app is running on port 5000
- Check firewall settings
- Verify CORS configuration

## 📱 Browser Support

- **Chrome** 60+
- **Firefox** 55+
- **Safari** 12+
- **Edge** 79+

## 🔄 Development

### Adding New Features
1. **Database changes**: Modify models in `learn.py`
2. **API endpoints**: Add routes in `learn.py`
3. **Frontend**: Update `index.html` and `main.css`
4. **Testing**: Test all user roles and workflows

### Database Schema
```sql
Users: id, username, email, password_hash, is_admin, created_at
Artworks: id, title, description, filename, artist_id, category_id, is_approved, is_featured
Votes: id, user_id, artwork_id, created_at
Categories: id, name, description
Comments: id, content, user_id, artwork_id, created_at
```

## 📄 License

This project is created for the St. Mark's Preaching Festival 2026. Please ensure appropriate usage rights and permissions.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## 📞 Support

For technical support or questions about the festival, please contact the festival organizers.

---

**مهرجان الكرازة المرقسية 2026** - Celebrating artistic expression in our community! 🎨✨
