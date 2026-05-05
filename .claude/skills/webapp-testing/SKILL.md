---
name: webapp-testing
description: Browser-based web application testing using Playwright for end-to-end, visual regression, and accessibility testing
version: 1.0.0
author: Claude Code Community
tags:
  - testing
  - e2e
  - playwright
  - browser
  - visual-regression
  - accessibility
---

# Web Application Testing

Comprehensive browser-based testing for web applications using Playwright.

## Prerequisites

- Python 3.8+ and pip
- Playwright: `pip install playwright && playwright install`
- Node.js (for running dev servers)

## Usage

When asked to test a web application:

### 1. Test Setup

```bash
# Create test directory
mkdir -p tests/e2e

# Initialize Playwright (if not already done)
pip install playwright pytest-playwright
playwright install
```

### 2. Basic Test Structure

```python
# tests/e2e/test_example.py
from playwright.sync_api import Page, expect

def test_homepage_loads(page: Page):
    page.goto("http://localhost:5173")
    expect(page).to_have_title(/OA System/)
```

### 3. Page Object Model Pattern

```python
# tests/e2e/pages/login_page.py
class LoginPage:
    def __init__(self, page: Page):
        self.page = page
        self.username_input = page.locator('[name="username"]')
        self.password_input = page.locator('[name="password"]')
        self.login_button = page.locator('button[type="submit"]')

    def navigate(self):
        self.page.goto("/login")

    def login(self, username: str, password: str):
        self.username_input.fill(username)
        self.password_input.fill(password)
        self.login_button.click()
```

### 4. Multi-Browser Testing

```python
# tests/e2e/conftest.py
import pytest
from playwright.sync_api import Playwright

@pytest.fixture(params=["chromium", "firefox", "webkit"])
def browser_type(request, playwright: Playwright):
    return getattr(playwright, request.param)
```

### 5. Visual Regression Testing

```python
def test_visual_regression(page: Page):
    page.goto("/dashboard")
    expect(page).to_have_screenshot("dashboard.png")
```

### 6. Mobile Emulation

```python
def test_mobile_view(page: Page):
    iphone = playwright.devices["iPhone 13"]
    context = browser.new_context(**iphone)
    page = context.new_page()
    page.goto("/")
```

### 7. Accessibility Testing

```python
def test_accessibility(page: Page):
    page.goto("/")
    # Check for common a11y issues
    assert page.locator("img[alt]").count() >= page.locator("img").count()
    assert page.locator("button:not([aria-label])").count() == 0
```

## Test Commands

```bash
# Run all e2e tests
pytest tests/e2e --headed

# Run specific test file
pytest tests/e2e/test_login.py

# Run with specific browser
pytest tests/e2e --browser=chromium

# Generate HTML report
pytest tests/e2e --html=report.html
```

## Best Practices

1. **Use Page Object Model**: Encapsulate page logic in reusable classes
2. **Wait for elements**: Use `expect()` instead of fixed waits
3. **Test isolation**: Each test should be independent
4. **Meaningful assertions**: Test user-visible behavior, not implementation
5. **Keep tests fast**: Use API calls for setup/teardown when possible

## Integration with CI/CD

```yaml
# .github/workflows/e2e.yml
- name: Run E2E tests
  run: |
    pip install playwright pytest-playwright
    playwright install --with-deps
    pytest tests/e2e
```
