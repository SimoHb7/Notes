const express = require('express');
const router = express.Router();
const pool = require('../db');
const auth = require('../middleware/auth');

// Get all notes for a user
router.get('/', auth, async (req, res) => {
    try {
        const { rows } = await pool.query(
            'SELECT * FROM notes WHERE user_id = $1 ORDER BY created_at DESC',
            [req.user.id]
        );
        res.json(rows);
    } catch (err) {
        console.error(err.message);
        res.status(500).json({ error: 'Server error' });
    }
});

// Create a new note
router.post('/', auth, async (req, res) => {
    try {
        const { title, content, category, priority, due_date } = req.body;
        
        // Validate required fields
        if (!title || !content) {
            return res.status(400).json({ error: 'Title and content are required' });
        }

        const { rows } = await pool.query(
            `INSERT INTO notes 
            (title, content, category, priority, due_date, user_id, created_at, updated_at) 
            VALUES ($1, $2, $3, $4, $5, $6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) 
            RETURNING *`,
            [title, content, category, priority, due_date, req.user.id]
        );

        res.status(201).json(rows[0]);
    } catch (err) {
        console.error(err.message);
        res.status(500).json({ error: 'Server error' });
    }
});

// Update a note
router.put('/:id', auth, async (req, res) => {
    try {
        const { id } = req.params;
        const { title, content, category, priority, due_date } = req.body;

        // First check if note exists and belongs to user
        const noteCheck = await pool.query(
            'SELECT * FROM notes WHERE id = $1 AND user_id = $2',
            [id, req.user.id]
        );

        if (noteCheck.rows.length === 0) {
            return res.status(404).json({ error: 'Note not found' });
        }

        // Update the note
        const { rows } = await pool.query(
            `UPDATE notes 
            SET title = $1, 
                content = $2, 
                category = $3, 
                priority = $4, 
                due_date = $5,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = $6 AND user_id = $7 
            RETURNING *`,
            [title, content, category, priority, due_date, id, req.user.id]
        );

        res.json(rows[0]);
    } catch (err) {
        console.error(err.message);
        res.status(500).json({ error: 'Server error' });
    }
});

// Delete a note
router.delete('/:id', auth, async (req, res) => {
    try {
        const { id } = req.params;

        // First check if note exists and belongs to user
        const noteCheck = await pool.query(
            'SELECT * FROM notes WHERE id = $1 AND user_id = $2',
            [id, req.user.id]
        );

        if (noteCheck.rows.length === 0) {
            return res.status(404).json({ error: 'Note not found' });
        }

        await pool.query('DELETE FROM notes WHERE id = $1 AND user_id = $2', [id, req.user.id]);
        res.json({ message: 'Note deleted successfully' });
    } catch (err) {
        console.error(err.message);
        res.status(500).json({ error: 'Server error' });
    }
});

module.exports = router; 