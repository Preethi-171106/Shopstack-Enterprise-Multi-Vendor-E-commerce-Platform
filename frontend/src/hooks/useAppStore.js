import { useSelector, useDispatch } from 'react-redux';

/**
 * Custom hook for simplified access to Redux app state & dispatch
 */
export const useAppStore = () => {
  const dispatch = useDispatch();
  const appState = useSelector((state) => state.app);

  return {
    ...appState,
    dispatch,
  };
};

export default useAppStore;
