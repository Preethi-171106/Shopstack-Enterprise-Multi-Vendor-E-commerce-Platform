import React, { useEffect } from 'react';
import { BrowserRouter } from 'react-router-dom';
import { Provider } from 'react-redux';
import { store } from './store';
import { AppProvider } from './context/AppContext';
import { AuthProvider } from './context/AuthContext';
import AppRoutes from './routes/AppRoutes';
import { initServerWakeup } from './services/serverWakeup';

function App() {
  useEffect(() => {
    // Silently ping backend on startup to wake up free-tier instance (e.g. Render)
    initServerWakeup();
  }, []);

  return (
    <Provider store={store}>
      <AppProvider>
        <AuthProvider>
          <BrowserRouter>
            <AppRoutes />
          </BrowserRouter>
        </AuthProvider>
      </AppProvider>
    </Provider>
  );
}

export default App;
