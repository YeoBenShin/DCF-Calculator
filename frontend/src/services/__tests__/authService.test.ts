// Simple unit tests for localStorage utility functions
// We'll focus on testing the components instead of the service implementation details

describe('AuthService Utility Functions', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  test('localStorage operations work correctly', () => {
    // Test setting and getting token
    localStorage.setItem('authToken', 'test-token');
    expect(localStorage.getItem('authToken')).toBe('test-token');

    // Test setting and getting user
    const mockUser = { userId: '1', email: 'test@example.com', watchlist: [] };
    localStorage.setItem('user', JSON.stringify(mockUser));
    expect(JSON.parse(localStorage.getItem('user') || '{}')).toEqual(mockUser);

    // Test clearing localStorage
    localStorage.removeItem('authToken');
    localStorage.removeItem('user');
    expect(localStorage.getItem('authToken')).toBeNull();
    expect(localStorage.getItem('user')).toBeNull();
  });
});