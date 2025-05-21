const express = require('express');
const router = express.Router();
const jwt = require('jsonwebtoken');

// Middleware to verify JWT token
const auth = async (req, res, next) => {
    try {
        const token = req.header('Authorization').replace('Bearer ', '');
        const decoded = jwt.verify(token, 'your_jwt_secret');
        req.userId = decoded.id;
        next();
    } catch (error) {
        res.status(401).json({ message: 'Please authenticate' });
    }
};

// Get all notes for a user
router.get('/', auth, async (req, res) => {
    try {
        const result = await req.app.locals.pool.query(
            'SELECT * FROM notes WHERE user_id = $1 ORDER BY created_at DESC',
            [req.userId]
        );
        res.json(result.rows);
    } catch (error) {
        console.error('Error fetching notes:', error);
        res.status(500).json({ message: 'Server error' });
    }
});

// Get a single note by ID
router.get('/:id', auth, async (req, res) => {
    try {
        const noteId = parseInt(req.params.id, 10);
        if (isNaN(noteId)) {
            return res.status(400).json({ message: 'Invalid note id' });
        }

        const result = await req.app.locals.pool.query(
            'SELECT * FROM notes WHERE id = $1 AND user_id = $2',
            [noteId, req.userId]
        );

        if (result.rows.length === 0) {
            return res.status(404).json({ message: 'Note not found' });
        }

        res.json(result.rows[0]);
    } catch (error) {
        console.error('Error fetching note:', error);
        res.status(500).json({ message: 'Server error' });
    }
});

// Create a new note
router.post('/', auth, async (req, res) => {
    try {
        const { title, content } = req.body;
        const result = await req.app.locals.pool.query(
            'INSERT INTO notes (user_id, title, content) VALUES ($1, $2, $3) RETURNING *',
            [req.userId, title, content]
        );
        res.status(201).json(result.rows[0]);
    } catch (error) {
        console.error('Error creating note:', error);
        res.status(500).json({ message: 'Server error' });
    }
});

// Update a note
router.put('/:id', auth, async (req, res) => {
    try {
        console.log('Update note request received:', {
            noteId: req.params.id,
            userId: req.userId,
            body: req.body
        });

        const { title, content } = req.body;
        const noteId = parseInt(req.params.id, 10);
        
        if (isNaN(noteId)) {
            console.log('Invalid note ID:', req.params.id);
            return res.status(400).json({ message: 'Invalid note id' });
        }

        if (!title || !content) {
            console.log('Missing required fields:', { title: !!title, content: !!content });
            return res.status(400).json({ message: 'Title and content are required' });
        }

        // First check if the note exists and belongs to the user
        console.log('Checking note ownership:', { noteId, userId: req.userId });
        const checkResult = await req.app.locals.pool.query(
            'SELECT * FROM notes WHERE id = $1 AND user_id = $2',
            [noteId, req.userId]
        );

        if (checkResult.rows.length === 0) {
            console.log('Note not found or unauthorized:', { noteId, userId: req.userId });
            return res.status(404).json({ message: 'Note not found' });
        }

        // Update the note
        console.log('Updating note:', { noteId, title, contentLength: content.length });
        const result = await req.app.locals.pool.query(
            'UPDATE notes SET title = $1, content = $2, updated_at = CURRENT_TIMESTAMP WHERE id = $3 AND user_id = $4 RETURNING *',
            [title, content, noteId, req.userId]
        );
        
        if (result.rows.length === 0) {
            console.log('Update failed - no rows affected');
            return res.status(404).json({ message: 'Note not found' });
        }
        
        console.log('Note updated successfully:', { noteId, title });
        res.json(result.rows[0]);
    } catch (error) {
        console.error('Error updating note:', error);
        console.error('Error details:', {
            message: error.message,
            stack: error.stack,
            code: error.code
        });
        res.status(500).json({ 
            message: 'Server error', 
            details: error.message,
            code: error.code
        });
    }
});

// Delete a note
router.delete('/:id', auth, async (req, res) => {
    try {
        const result = await req.app.locals.pool.query(
            'DELETE FROM notes WHERE id = $1 AND user_id = $2 RETURNING *',
            [req.params.id, req.userId]
        );
        
        if (result.rows.length === 0) {
            return res.status(404).json({ message: 'Note not found' });
        }
        
        res.json({ message: 'Note deleted successfully' });
    } catch (error) {
        console.error('Error deleting note:', error);
        res.status(500).json({ message: 'Server error' });
    }
});

module.exports = router; 