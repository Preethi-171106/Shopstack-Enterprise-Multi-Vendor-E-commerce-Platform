import React from 'react';
import { Outlet } from 'react-router-dom';
import Header from '../components/Header';
import Footer from '../components/Footer';
import Toast from '../components/common/Toast';

const MainLayout = () => {
  return (
    <div className="min-h-screen flex flex-col bg-slate-900 text-slate-100 selection:bg-indigo-500 selection:text-white">
      {/* Header Application Shell */}
      <Header />

      {/* Main Content Area */}
      <main className="flex-grow flex flex-col">
        <Outlet />
      </main>

      {/* Footer Application Shell */}
      <Footer />

      {/* Global Toast Notification System */}
      <Toast />
    </div>
  );
};

export default MainLayout;
