const express = require('express');
const router = express.Router();
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { Pool } = require('pg');

const pool = new Pool({
    user: 'postgres',
    host: 'localhost',
    database: 'notes_db',
    password: '1234', 
    port: 5432,
});

router.post('/register', async (req, res) => {
    try {
        const { email, password, name } = req.body;

        const userCheck = await pool.query(
            'SELECT * FROM users WHERE email = $1',
            [email]
        );

        if (userCheck.rows.length > 0) {
            return res.status(400).json({ error: 'User already exists' });
        }

        const salt = await bcrypt.genSalt(10);
        const hashedPassword = await bcrypt.hash(password, salt);

        const result = await pool.query(
            'INSERT INTO users (email, password, name) VALUES ($1, $2, $3) RETURNING id, email, name',
            [email, hashedPassword, name]
        );

        const token = jwt.sign(
            { id: result.rows[0].id },
            'your_jwt_secret',
            { expiresIn: '24h' }
        );

        res.header('Authorization', `Bearer ${token}`);
        res.status(201).json({
            message: 'User registered successfully',
            user: result.rows[0]
        });
    } catch (error) {
        console.error('Registration error:', error);
        res.status(500).json({ error: 'Server error' });
    }
});

router.post('/login', async (req, res) => {
    try {
        const { email, password } = req.body;

        const result = await pool.query(
            'SELECT * FROM users WHERE email = $1',
            [email]
        );

        if (result.rows.length === 0) {
            return res.status(400).json({ error: 'Invalid credentials' });
        }

        const user = result.rows[0];

        const isMatch = await bcrypt.compare(password, user.password);
        if (!isMatch) {
            return res.status(400).json({ error: 'Invalid credentials' });
        }

        const token = jwt.sign(
            { id: user.id },
            'your_jwt_secret',
            { expiresIn: '24h' }
        );

        res.header('Authorization', `Bearer ${token}`);
        res.json({
            message: 'Login successful',
            user: {
                id: user.id,
                email: user.email,
                name: user.name
            }
        });
    } catch (error) {
        console.error('Login error:', error);
        res.status(500).json({ error: 'Server error' });
    }
});

module.exports = router;
