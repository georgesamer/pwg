from flask import Flask, request, jsonify, session, render_template, send_from_directory
from flask_sqlalchemy import SQLAlchemy
from flask_cors import CORS
from werkzeug.security import generate_password_hash, check_password_hash
from werkzeug.utils import secure_filename
from datetime import datetime, date
import os
import uuid
from functools import wraps

# Ensure Flask serves the local `static` and `templates` directories inside this project
app = Flask(__name__, static_folder='static', template_folder='templates')
app.config['SECRET_KEY'] = 'your-secret-key-change-this'
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///festival_art.db'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
app.config['UPLOAD_FOLDER'] = os.path.join('static', 'uploads')
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024  # 16MB max file size

db = SQLAlchemy(app)
CORS(app, supports_credentials=True)

# Ensure upload directory exists
os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)

# Database Models
class User(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(80), unique=True, nullable=False)
    email = db.Column(db.String(120), unique=True, nullable=False)
    password_hash = db.Column(db.String(128), nullable=False)
    is_admin = db.Column(db.Boolean, default=False)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    
    # Relationships
    artworks = db.relationship('Artwork', backref='artist', lazy=True)
    votes = db.relationship('Vote', backref='voter', lazy=True)
    comments = db.relationship('Comment', backref='author', lazy=True)

class Category(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(100), unique=True, nullable=False)
    description = db.Column(db.Text)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    
    # Relationships
    artworks = db.relationship('Artwork', backref='category', lazy=True)

class Artwork(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    title = db.Column(db.String(200), nullable=False)
    description = db.Column(db.Text)
    filename = db.Column(db.String(255), nullable=False)
    original_filename = db.Column(db.String(255))
    file_path = db.Column(db.String(500), nullable=False)
    artist_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False)
    category_id = db.Column(db.Integer, db.ForeignKey('category.id'))
    is_featured = db.Column(db.Boolean, default=False)
    is_approved = db.Column(db.Boolean, default=False)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    updated_at = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    
    # Relationships
    votes = db.relationship('Vote', backref='artwork', lazy=True, cascade='all, delete-orphan')
    comments = db.relationship('Comment', backref='artwork', lazy=True, cascade='all, delete-orphan')
    
    @property
    def vote_count(self):
        return len(self.votes)
    
    @property
    def comment_count(self):
        return len(self.comments)

class Vote(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    user_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False)
    artwork_id = db.Column(db.Integer, db.ForeignKey('artwork.id'), nullable=False)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    
    __table_args__ = (db.UniqueConstraint('user_id', 'artwork_id', name='unique_vote'),)

class Comment(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    content = db.Column(db.Text, nullable=False)
    user_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False)
    artwork_id = db.Column(db.Integer, db.ForeignKey('artwork.id'), nullable=False)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)

class FestivalSettings(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    key = db.Column(db.String(100), unique=True, nullable=False)
    value = db.Column(db.Text)
    updated_at = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

# Helper Functions
def login_required(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        if 'user_id' not in session:
            return jsonify({'error': 'Authentication required'}), 401
        return f(*args, **kwargs)
    return decorated_function

def admin_required(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        if 'user_id' not in session:
            return jsonify({'error': 'Authentication required'}), 401
        
        user = User.query.get(session['user_id'])
        if not user or not user.is_admin:
            return jsonify({'error': 'Admin access required'}), 403
        return f(*args, **kwargs)
    return decorated_function

def allowed_file(filename):
    ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'webp'}
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

# Authentication Routes
@app.route('/api/auth/register', methods=['POST'])
def register():
    data = request.get_json()
    
    # Validate input
    if not data.get('username') or not data.get('email') or not data.get('password'):
        return jsonify({'error': 'Username, email, and password are required'}), 400
    
    # Check if user already exists
    if User.query.filter_by(username=data['username']).first():
        return jsonify({'error': 'Username already exists'}), 409
    
    if User.query.filter_by(email=data['email']).first():
        return jsonify({'error': 'Email already registered'}), 409
    
    # Create new user
    user = User(
        username=data['username'],
        email=data['email'],
        password_hash=generate_password_hash(data['password'])
    )
    
    db.session.add(user)
    db.session.commit()
    
    # Auto-login after registration
    session['user_id'] = user.id
    session['username'] = user.username
    session['is_admin'] = user.is_admin
    
    return jsonify({
        'message': 'Registration successful',
        'user': {
            'id': user.id,
            'username': user.username,
            'email': user.email,
            'is_admin': user.is_admin
        }
    }), 201

@app.route('/api/auth/login', methods=['POST'])
def login():
    data = request.get_json()
    
    if not data.get('username') or not data.get('password'):
        return jsonify({'error': 'Username and password are required'}), 400
    
    user = User.query.filter_by(username=data['username']).first()
    
    if user and check_password_hash(user.password_hash, data['password']):
        session['user_id'] = user.id
        session['username'] = user.username
        session['is_admin'] = user.is_admin
        
        return jsonify({
            'message': 'Login successful',
            'user': {
                'id': user.id,
                'username': user.username,
                'email': user.email,
                'is_admin': user.is_admin
            }
        })
    
    return jsonify({'error': 'Invalid username or password'}), 401

@app.route('/api/auth/logout', methods=['POST'])
@login_required
def logout():
    session.clear()
    return jsonify({'message': 'Logout successful'})

@app.route('/api/auth/me', methods=['GET'])
@login_required
def get_current_user():
    user = User.query.get(session['user_id'])
    return jsonify({
        'user': {
            'id': user.id,
            'username': user.username,
            'email': user.email,
            'is_admin': user.is_admin
        }
    })

# Category Routes
@app.route('/api/categories', methods=['GET'])
def get_categories():
    categories = Category.query.all()
    return jsonify({
        'categories': [{
            'id': cat.id,
            'name': cat.name,
            'description': cat.description,
            'artwork_count': len(cat.artworks)
        } for cat in categories]
    })

@app.route('/api/categories', methods=['POST'])
@admin_required
def create_category():
    data = request.get_json()
    
    if not data.get('name'):
        return jsonify({'error': 'Category name is required'}), 400
    
    if Category.query.filter_by(name=data['name']).first():
        return jsonify({'error': 'Category already exists'}), 409
    
    category = Category(
        name=data['name'],
        description=data.get('description', '')
    )
    
    db.session.add(category)
    db.session.commit()
    
    return jsonify({
        'message': 'Category created successfully',
        'category': {
            'id': category.id,
            'name': category.name,
            'description': category.description
        }
    }), 201

# Artwork Routes
@app.route('/api/artworks', methods=['GET'])
def get_artworks():
    page = request.args.get('page', 1, type=int)
    per_page = request.args.get('per_page', 12, type=int)
    category_id = request.args.get('category_id', type=int)
    featured_only = request.args.get('featured', type=bool)
    sort_by = request.args.get('sort', 'recent')  # recent, popular, title
    
    query = Artwork.query.filter_by(is_approved=True)
    
    if category_id:
        query = query.filter_by(category_id=category_id)
    
    if featured_only:
        query = query.filter_by(is_featured=True)
    
    # Sorting
    if sort_by == 'popular':
        # Sort by vote count (requires a subquery)
        query = query.outerjoin(Vote).group_by(Artwork.id).order_by(db.func.count(Vote.id).desc())
    elif sort_by == 'title':
        query = query.order_by(Artwork.title.asc())
    else:  # recent
        query = query.order_by(Artwork.created_at.desc())
    
    artworks = query.paginate(page=page, per_page=per_page, error_out=False)
    
    return jsonify({
        'artworks': [{
            'id': artwork.id,
            'title': artwork.title,
            'description': artwork.description,
            'filename': artwork.filename,
            'file_path': f'/uploads/{artwork.filename}',
            'artist': {
                'id': artwork.artist.id,
                'username': artwork.artist.username
            },
            'category': {
                'id': artwork.category.id,
                'name': artwork.category.name
            } if artwork.category else None,
            'vote_count': artwork.vote_count,
            'comment_count': artwork.comment_count,
            'is_featured': artwork.is_featured,
            'created_at': artwork.created_at.isoformat()
        } for artwork in artworks.items],
        'pagination': {
            'page': artworks.page,
            'pages': artworks.pages,
            'per_page': artworks.per_page,
            'total': artworks.total,
            'has_next': artworks.has_next,
            'has_prev': artworks.has_prev
        }
    })

@app.route('/api/artworks/<int:artwork_id>', methods=['GET'])
def get_artwork(artwork_id):
    artwork = Artwork.query.filter_by(id=artwork_id, is_approved=True).first()
    if not artwork:
        return jsonify({'error': 'Artwork not found'}), 404
    
    return jsonify({
        'artwork': {
            'id': artwork.id,
            'title': artwork.title,
            'description': artwork.description,
            'filename': artwork.filename,
            'file_path': f'/uploads/{artwork.filename}',
            'artist': {
                'id': artwork.artist.id,
                'username': artwork.artist.username
            },
            'category': {
                'id': artwork.category.id,
                'name': artwork.category.name
            } if artwork.category else None,
            'vote_count': artwork.vote_count,
            'comment_count': artwork.comment_count,
            'is_featured': artwork.is_featured,
            'created_at': artwork.created_at.isoformat()
        }
    })

@app.route('/api/artworks', methods=['POST'])
@login_required
def upload_artwork():
    if 'file' not in request.files:
        return jsonify({'error': 'No file uploaded'}), 400
    
    file = request.files['file']
    title = request.form.get('title')
    description = request.form.get('description', '')
    category_id = request.form.get('category_id', type=int)
    
    if not title:
        return jsonify({'error': 'Title is required'}), 400
    
    if file.filename == '':
        return jsonify({'error': 'No file selected'}), 400
    
    if not allowed_file(file.filename):
        return jsonify({'error': 'Invalid file type'}), 400
    
    # Generate unique filename
    original_filename = secure_filename(file.filename)
    unique_filename = f"{uuid.uuid4().hex}_{original_filename}"
    file_path = os.path.join(app.config['UPLOAD_FOLDER'], unique_filename)
    
    # Save file
    file.save(file_path)
    
    # Create artwork record
    artwork = Artwork(
        title=title,
        description=description,
        filename=unique_filename,
        original_filename=original_filename,
        file_path=file_path,
        artist_id=session['user_id'],
        category_id=category_id,
        is_approved=False  # Requires admin approval
    )
    
    db.session.add(artwork)
    db.session.commit()
    
    return jsonify({
        'message': 'Artwork uploaded successfully and is pending approval',
        'artwork_id': artwork.id,
        'file_url': f"/static/uploads/{unique_filename}"
    }), 201

# Voting Routes
@app.route('/api/artworks/<int:artwork_id>/vote', methods=['POST'])
@login_required
def vote_artwork(artwork_id):
    artwork = Artwork.query.filter_by(id=artwork_id, is_approved=True).first()
    if not artwork:
        return jsonify({'error': 'Artwork not found'}), 404
    
    # Check if user already voted
    existing_vote = Vote.query.filter_by(user_id=session['user_id'], artwork_id=artwork_id).first()
    if existing_vote:
        return jsonify({'error': 'You have already voted for this artwork'}), 409
    
    # Create vote
    vote = Vote(user_id=session['user_id'], artwork_id=artwork_id)
    db.session.add(vote)
    db.session.commit()
    
    return jsonify({
        'message': 'Vote recorded successfully',
        'vote_count': artwork.vote_count
    })

@app.route('/api/artworks/<int:artwork_id>/vote', methods=['DELETE'])
@login_required
def remove_vote(artwork_id):
    vote = Vote.query.filter_by(user_id=session['user_id'], artwork_id=artwork_id).first()
    if not vote:
        return jsonify({'error': 'Vote not found'}), 404
    
    db.session.delete(vote)
    db.session.commit()
    
    artwork = Artwork.query.get(artwork_id)
    return jsonify({
        'message': 'Vote removed successfully',
        'vote_count': artwork.vote_count if artwork else 0
    })

# Statistics Routes
@app.route('/api/statistics', methods=['GET'])
def get_statistics():
    total_artworks = Artwork.query.filter_by(is_approved=True).count()
    total_votes = Vote.query.count()
    active_participants = User.query.count()
    total_comments = Comment.query.count()
    
    return jsonify({
        'statistics': {
            'total_artworks': total_artworks,
            'total_votes': total_votes,
            'active_participants': active_participants,
            'total_comments': total_comments
        }
    })

@app.route('/api/top-voted', methods=['GET'])
def get_top_voted():
    limit = request.args.get('limit', 10, type=int)
    
    # Query top voted artworks
    top_artworks = (db.session.query(Artwork)
                   .join(Vote)
                   .filter(Artwork.is_approved == True)
                   .group_by(Artwork.id)
                   .order_by(db.func.count(Vote.id).desc())
                   .limit(limit)
                   .all())
    
    return jsonify({
        'top_voted': [{
            'id': artwork.id,
            'title': artwork.title,
            'artist': artwork.artist.username,
            'vote_count': artwork.vote_count,
            'file_path': f'/uploads/{artwork.filename}'
        } for artwork in top_artworks]
    })

# Admin Routes
@app.route('/api/admin/artworks', methods=['GET'])
@admin_required
def admin_get_artworks():
    page = request.args.get('page', 1, type=int)
    per_page = request.args.get('per_page', 20, type=int)
    status = request.args.get('status')  # pending, approved, all
    
    query = Artwork.query
    
    if status == 'pending':
        query = query.filter_by(is_approved=False)
    elif status == 'approved':
        query = query.filter_by(is_approved=True)
    
    artworks = query.order_by(Artwork.created_at.desc()).paginate(
        page=page, per_page=per_page, error_out=False
    )
    
    return jsonify({
        'artworks': [{
            'id': artwork.id,
            'title': artwork.title,
            'description': artwork.description,
            'artist': artwork.artist.username,
            'category': artwork.category.name if artwork.category else None,
            'is_approved': artwork.is_approved,
            'is_featured': artwork.is_featured,
            'vote_count': artwork.vote_count,
            'created_at': artwork.created_at.isoformat(),
            'file_path': f'/uploads/{artwork.filename}'
        } for artwork in artworks.items],
        'pagination': {
            'page': artworks.page,
            'pages': artworks.pages,
            'per_page': artworks.per_page,
            'total': artworks.total
        }
    })

@app.route('/api/admin/artworks/<int:artwork_id>/approve', methods=['PUT'])
@admin_required
def approve_artwork(artwork_id):
    artwork = Artwork.query.get_or_404(artwork_id)
    artwork.is_approved = True
    db.session.commit()
    
    return jsonify({'message': 'Artwork approved successfully'})

@app.route('/api/admin/artworks/<int:artwork_id>/feature', methods=['PUT'])
@admin_required
def toggle_featured(artwork_id):
    artwork = Artwork.query.get_or_404(artwork_id)
    artwork.is_featured = not artwork.is_featured
    db.session.commit()
    
    status = 'featured' if artwork.is_featured else 'unfeatured'
    return jsonify({'message': f'Artwork {status} successfully'})

# Initialize database
def create_tables():
    db.create_all()
    
    # Create default categories if they don't exist
    default_categories = [
        {'name': 'Paintings', 'description': 'Traditional and digital paintings'},
        {'name': 'Photography', 'description': 'Photographic works'},
        {'name': 'Sculptures', 'description': '3D artistic works'},
        {'name': 'Digital Art', 'description': 'Computer-generated artwork'},
        {'name': 'Mixed Media', 'description': 'Art using multiple mediums'}
    ]
    
    for cat_data in default_categories:
        if not Category.query.filter_by(name=cat_data['name']).first():
            category = Category(**cat_data)
            db.session.add(category)
    
    db.session.commit()

# Error handlers
@app.errorhandler(404)
def not_found(error):
    return jsonify({'error': 'Resource not found'}), 404

@app.errorhandler(500)
def internal_error(error):
    db.session.rollback()
    return jsonify({'error': 'Internal server error'}), 500
@app.route('/')
def index():
    return render_template('index.html')


@app.route('/uploads/<path:filename>')
def uploaded_file(filename):
    return send_from_directory(app.config['UPLOAD_FOLDER'], filename)


if __name__ == '__main__':
    # Create DB and default categories on first run (inside app context)
    with app.app_context():
        create_tables()
    app.run(debug=True, host='0.0.0.0', port=5000)
