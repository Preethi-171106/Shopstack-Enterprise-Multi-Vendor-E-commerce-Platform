import React from 'react';
import { Link } from 'react-router-dom';
import { Home as HomeIcon } from 'lucide-react';

const NotFound = () => {
  return (
    <div className="flex-1 flex flex-col items-center justify-center p-6 text-center">
      <h1 className="text-7xl font-extrabold text-indigo-400">404</h1>
      <h2 className="text-2xl font-bold text-white mt-4">Page Not Found</h2>
      <p className="text-slate-400 mt-2 max-w-md">
        The requested URL was not found in ShopStack routing system.
      </p>
      <Link
        to="/"
        className="mt-6 inline-flex items-center space-x-2 px-5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-medium text-sm transition-all"
      >
        <HomeIcon className="w-4 h-4" />
        <span>Return to Welcome Screen</span>
      </Link>
    </div>
  );
};

export default NotFound;
