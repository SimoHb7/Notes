require('dotenv').config();
const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');
const { Pool } = require('pg');
const auth = require('./middleware/auth');

const app = express();
const port = process.env.PORT || 3000;

app.use(cors());
app.use(bodyParser.json());

const pool = new Pool({
    host: process.env.DB_HOST,
    port: process.env.DB_PORT,
    database: process.env.DB_NAME,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD
});

pool.connect((err, client, release) => {
    if (err) {
        console.error('Error connecting to the database:', err);
    } else {
        console.log('Successfully connected to database');
        release();
    }
});

app.get('/', (req, res) => {
    res.json({ message: 'Notes API is running' });
});

app.get('/api/notes', auth, async (req, res) => {
    try {
        const result = await pool.query(
            'SELECT * FROM notes WHERE user_id = $1 ORDER BY created_at DESC',
            [req.user.id]
        );
        res.json(result.rows);
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Internal server error' });
    }
});

app.get('/api/notes/:id', auth, async (req, res) => {
    try {
        const { id } = req.params;
        const result = await pool.query(
            'SELECT * FROM notes WHERE id = $1 AND user_id = $2',
            [id, req.user.id]
        );
        
        if (result.rows.length === 0) {
            return res.status(404).json({ error: 'Note not found' });
        }
        
        res.json(result.rows[0]);
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Internal server error' });
    }
});

app.post('/api/notes', auth, async (req, res) => {
    try {
        const { title, content } = req.body;
        
        if (!title || !content) {
            return res.status(400).json({ error: 'Title and content are required' });
        }
        
        const result = await pool.query(
            'INSERT INTO notes (title, content, user_id, created_at, updated_at) VALUES ($1, $2, $3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) RETURNING *',
            [title, content, req.user.id]
        );
        
        res.status(201).json(result.rows[0]);
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Internal server error' });
    }
});

app.put('/api/notes/:id', auth, async (req, res) => {
    try {
        const { id } = req.params;
        const { title, content } = req.body;
        
        if (!title || !content) {
            return res.status(400).json({ error: 'Title and content are required' });
        }
        
        const noteCheck = await pool.query(
            'SELECT * FROM notes WHERE id = $1 AND user_id = $2',
            [id, req.user.id]
        );

        if (noteCheck.rows.length === 0) {
            return res.status(404).json({ error: 'Note not found' });
        }
        
        const result = await pool.query(
            'UPDATE notes SET title = $1, content = $2, updated_at = CURRENT_TIMESTAMP WHERE id = $3 AND user_id = $4 RETURNING *',
            [title, content, id, req.user.id]
        );
        
        res.json(result.rows[0]);
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Internal server error' });
    }
});

app.delete('/api/notes/:id', auth, async (req, res) => {
    try {
        const { id } = req.params;
        
        const noteCheck = await pool.query(
            'SELECT * FROM notes WHERE id = $1 AND user_id = $2',
            [id, req.user.id]
        );

        if (noteCheck.rows.length === 0) {
            return res.status(404).json({ error: 'Note not found' });
        }
        
        await pool.query('DELETE FROM notes WHERE id = $1 AND user_id = $2', [id, req.user.id]);
        res.status(204).send();
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Internal server error' });
    }
});

const authRoutes = require('./routes/auth');
app.use('/auth', authRoutes);

// Start server
app.listen(port, '0.0.0.0', () => {
    console.log(`Server running on port ${port}`);
    console.log(`Test the API at: http://localhost:${port}/api/notes`);
});
