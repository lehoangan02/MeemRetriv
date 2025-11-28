import React, { useState } from 'react';
import './App.css';

function App() {
  const [selectedImage, setSelectedImage] = useState(null);
  const [imagePreview, setImagePreview] = useState(null);
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // 1. Handle Image Selection
  const handleImageChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      setSelectedImage(file);
      setImagePreview(URL.createObjectURL(file));
      setResults([]); // Clear previous results
    }
  };

  // 2. Send to Java Backend
  const handleSearch = async (e) => {
    e.preventDefault();
    if (!selectedImage) return;

    setLoading(true);
    setError('');

    const formData = new FormData();
    formData.append('image', selectedImage); // Key must match Java @RequestParam

    try {
      // POINT THIS TO YOUR JAVA CONTROLLER
      const response = await fetch('http://localhost:8080/api/search', {
        method: 'POST',
        body: formData,
      });

      if (!response.ok) throw new Error('Backend connection failed');

      const data = await response.json();
      setResults(data);
    } catch (err) {
      console.error(err);
      setError('Could not connect to Java Backend. Is App.java running?');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="app-container">
      <header className="header">
        <div className="logo">🧱</div>
        <h1>Lego Neural Search</h1>
        <p>Powered by Weaviate Vector Database</p>
      </header>

      <main>
        {/* Upload Section */}
        <div className="upload-card">
          <div className="upload-area">
            <input 
              type="file" 
              accept="image/*" 
              id="file-upload" 
              onChange={handleImageChange} 
              hidden 
            />
            <label htmlFor="file-upload" className="upload-label">
              {imagePreview ? (
                <img src={imagePreview} alt="Preview" className="preview-img" />
              ) : (
                <div className="placeholder">
                  <span>📷</span>
                  <p>Click to upload LEGO piece</p>
                </div>
              )}
            </label>
          </div>
          
          <button 
            className="search-btn" 
            onClick={handleSearch} 
            disabled={!selectedImage || loading}
          >
            {loading ? 'Vectorizing & Searching...' : 'Identify Part'}
          </button>
          
          {error && <p className="error-msg">{error}</p>}
        </div>

        {/* Results Section */}
        <div className="results-container">
          {results.map((item, index) => (
            <div key={index} className="result-card">
              <div className="certainty-badge">
                {(item.certainty * 100).toFixed(1)}% Match
              </div>
              <img src={item.imgUrl} alt={item.name} />
              <div className="info">
                <h3>{item.name}</h3>
                <p>ID: {item.id}</p>
                <p className="description">{item.description}</p>
              </div>
            </div>
          ))}
        </div>
      </main>
    </div>
  );
}

export default App;