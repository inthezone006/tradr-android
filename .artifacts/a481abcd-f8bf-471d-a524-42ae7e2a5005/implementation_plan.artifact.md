# Implementation Plan - UX Modernization (Robinhood Style)

This plan overhauls the Sign In and Sign Up screens to follow modern fintech design patterns, emphasizing minimalism, bold typography, and improved ergonomics.

## Proposed Changes

### UI Components

#### [NEW] Custom Components
- **`ModernTextField`**: A custom input field with a sleek animated underline, floating labels, and a `RichBlack` focus state.
- **`PillButton`**: A modern, high-contrast action button designed for the bottom of the screen.

---

### Authentication Screens

#### [MODIFY] [LoginScreen.kt](file:///app/src/main/java/com/rahul/stocksim/ui/screens/LoginScreen.kt)
- **Layout**: Switch to a `Column` with `Arrangement.SpaceBetween`.
- **Header**: Position the "tradr." logo and a new bold "Welcome back" headline at the top.
- **Inputs**: Replace `OutlinedTextField` with `ModernTextField`.
- **Action**: Move the "Sign In" button to the bottom as a full-width `PillButton`.
- **Google Auth**: Redesign as a clean, secondary pill button above the main action.

#### [MODIFY] [RegisterScreen.kt](file:///app/src/main/java/com/rahul/stocksim/ui/screens/RegisterScreen.kt)
- **Layout**: Match the new Login layout for consistency.
- **Header**: Bold "Create Account" headline.
- **Inputs**: Use `ModernTextField` for Name and Email.
- **Action**: Move "Continue" to the bottom.

---

### Theme & Styling

#### [MODIFY] [Theme.kt](file:///app/src/main/java/com/rahul/stocksim/ui/theme/Theme.kt)
- Update `DarkColorScheme` primary color to a more vibrant "Trading Green" or "Electric Blue" if desired, or stay with the current palette for branding.

## Verification Plan

### Manual Verification
- **Visual Check**:
    - Confirm the new "Underline" input style looks sleek against the Jet Black background.
    - Verify the "Pill" buttons are correctly positioned at the bottom and are easy to tap.
    - Check that typography is bold and legible.
- **Interaction Check**:
    - Verify that keyboard focus doesn't obscure the bottom buttons (using `imePadding`).
    - Test the navigation flow between Sign In and Sign Up.
