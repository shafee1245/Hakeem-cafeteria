
--- Guide for same-document-transitions ---
# Same Document Transitions

## The Problem

Web sites often provide multiple views of an object, for instance a list of products, and then a detail page for each product. Navigating between the two views often feels disconnected. When a user clicks a product thumbnail to view its details, the thumbnail disappears and a new, larger image appears instantly elsewhere on the screen. This lack of continuity makes it harder for users to track relationships between elements.

## The Solution

The **View Transitions API** allows you to specify element pairs that exist in different states before and after a transition. When triggering a transition with `document.startViewTransition()` in a Single Page Navigation (SPA), the browser identifies these shared elements by their shared unique `view-transition-name`. It then automatically calculates the difference in their position, size, and styling, and animates them smoothly from the old state to the new state. This transition occurs in the top layer, above even elements with high `z-index` values.

## Implementation Guide

### Step 1: Wrap State Changes in `startViewTransition`

For Single-Page Applications (SPAs) or simple state changes, wrap the logic that updates the DOM in `document.startViewTransition`. The browser captures a snapshot of the current state, runs the update, and then captures the new state. 

```javascript
function navigate(view) {
  // MANDATORY: Wrap the update in startViewTransition
  document.startViewTransition(() => updateDOM(view));
}
```

### Step 2: Assign Shared Transition Names

Use the `view-transition-name` CSS property to tell the browser which elements should be morphed. The name can be anything (except `none`). **MANDATORY**: there must be no more than 1 element before and after with a given `view-transition-name`. If there are 2 or more elements with a given `view-transition-name`, the DOM will be updated to the new state immediately, without a transition.

You can use multiple `view-transition-name`s to morph multiple pairs of elements. For example, you may want to transition both the product image and title with separate transitions.

Because there are multiple items on the list view, you can not give the all of them the same `view-transition-name`. This can be solved in two ways in a SPA.

1. **Dynamic detail page:** Assign each item on the list page a unique `view-transition-name`, and then dynamically apply that name to the matching element on the detail page when the list item is selected, as shown here.

```css
/* In the list view, give each */
#product-1 { view-transition-name: p1 }
#product-2 { view-transition-name: p2 }
#product-3 { view-transition-name: p3 }
```

```js
function updateDOM(clickedTransitionName){
  const hero = document.getElementById("hero");
  hero.style.viewTransitionName = clickedTransitionName;
}
```

2. **Dynamic list item:** Assign the element on the detail page a `view-transition-name`, and apply that name to the item on the list page when it is selected. Remove the `view-transition-name` from the item on the list page when returning to the list page.

The `#hero` element on the detail page and the selected `.thumbnail` element on the list page share a `view-transition-name`. 

```css
#hero{
  view-transition-name: hero;
}
.thumbnail.selected {
  view-transition-name: hero;
}
```

When a thumbnail is clicked, we need to prepare the list view by assigning the `view-transition-name` using the `.selected` class selector, and making any changes to the DOM before starting the transition.

Then, you can call `document.startViewTransition()`, and apply the changes to transition the page from the detail to list view.

After navigating back to the list view, you must clean up the view transition classes to prevent the next navigation from erroring. You can perform this cleanup after the transition's `finished` promise resolves.

```javascript
// Function called when a thumbnail is clicked
function goFromListToDetail(e){
  e.currentTarget.classList.add("selected");
  const hero = document.getElementById("hero");
  const bgColor = getComputedStyle(e.currentTarget).backgroundColor;
  hero.style.background = bgColor;

  // Trigger the transition, checking for support
  if (!document.startViewTransition) {
    document.body.classList.add("detail");
    // MANDATORY Accessibility Routing: Route focus to the newly revealed heading to announce context and preserve logical tab flow
    document.getElementById("detail-heading")?.focus();
    return; // MANDATORY: End function execution if view transitions are not supported.  
  }
  const transition = document.startViewTransition(() => {
    document.body.classList.add("detail");
  });
  // MANDATORY Accessibility Routing: Route focus after the view transition resolves
  transition.finished.finally(() => {
    document.getElementById("detail-heading")?.focus();
  });
}

// Function called when navigating from detail back to list view
function goFromDetailToList() {
  if (!document.startViewTransition) {
    document.body.classList.remove("detail");
    document.getElementById("list-heading")?.focus();
    return;
  }
  const transition = document.startViewTransition(() => {
    document.body.classList.remove("detail");
  });
  // Clean up the list view and route focus
  transition.finished.finally(() => {
    // Route focus back to list view
    document.getElementById("list-heading")?.focus();
    // Remove selected classList to remove view-transition-names
    document.querySelectorAll(".selected").forEach(
      (element) => {
        element.classList.remove("selected");
      },
    );
  });
}
```

The method you choose will depend on the use case. The dynamic list item requires less repeated CSS, but more manual JavaScript cleanup.


### Step 3: Fix Aspect Ratio "Stretching"

By default, the browser cross-fades the old and new snapshots within a group that stretches to fit both. If you are transitioning text, set the width of the text element to `fit-content` on both the old and new views, so that the transitioned element's aspect ratio is stable.

```css
#list-page .title {
  width: fit-content;
}

#detail-page #title {
  width: fit-content;
}
```

If you are transitioning elements that change aspect ratio, you may need to set the height of the old and new pseudo-elements to 100% of the `::view-transition-pair()` pseudo-element.

```css
::view-transition-old(hero),
::view-transition-new(hero){
  height: 100%;
}
```

The pseudo-elements are snapshots of the live elements, so you can also use `object-fit` and `object-position` declarations for more control of the transitioning effect.

## Best Practices

-   **DO NOT** specify too many transitions. Only use shared elements for primary content that the user is actively tracking (e.g., hero images, headings).
-   **DO** remove temporary `view-transition-name` values after the transition finishes to avoid side effects on future transitions.
-   **DO NOT** transition elements with active animations. View transitions operate on snapshots, so any animations will appear to be paused during the view transition.
-   **DO** respect user preferences for reduced motion using the `prefers-reduced-motion` media query.
-   **MANDATORY Accessibility Routing**: View transitions morph page layouts dynamically but do not manage programmatic focus. If focus remains on an element that is hidden or removed during the transition, focus is abandoned, leaving keyboard and assistive technology users without context. Shift focus programmatically to an updated page heading or view container (using `tabindex="-1"`) immediately after the DOM updates or when the view transition's `finished` promise resolves.

```css
@media (prefers-reduced-motion: reduce) {
  ::view-transition-group(*),
  ::view-transition-old(*),
  ::view-transition-new(*) {
    animation: none !important;
  }
}
```

## Fallback Strategies

Baseline status for View transitions: Newly available. It's been Baseline since 2025-10-14.
Supported by: Chrome 111 (Mar 2023), Edge 111 (Mar 2023), Firefox 144 (Oct 2025), and Safari 18 (Sep 2024).

The View Transitions API is designed for progressive enhancement. Browsers that do not support it will simply execute the DOM update immediately without animation.

```javascript
function navigate(){
  if (!document.startViewTransition) {
    // Fallback: Just update the DOM
    updateDOM();
  } else {
    document.startViewTransition(() => updateDOM());
  }
}
```


--- Guide for cross-document-transitions ---
Cross-document view transitions allow you to create smooth, app-like transitions between different pages of a Multi-Page Application (MPA). By default, the browser performs a cross-fade, but you can customize this to match your site's aesthetic.

### Implementation Steps

#### 1. Opt-in to Cross-Document View Transitions

Both the source and destination pages must opt-in to view transitions for the browser to trigger them on navigation.

```css
/* Respect user's preference for reduced motion */
@media (prefers-reduced-motion: no-preference) {
  /* Add to a global stylesheet shared by both pages */
  @view-transition {
    /* Enables transitions for same-origin navigations */
    navigation: auto;
  }
}
```

#### 2. Customize Transition Animations (Optional)

You can target the old and new states of the transition using pseudo-elements to create effects like slides or reveals.

```css
/* Customizing the outgoing page animation */
::view-transition-old(root) {
  /* Move the old page out to the left */
  animation: 0.4s ease-in both slide-out;
}

/* Customizing the incoming page animation */
::view-transition-new(root) {
  /* Move the new page in from the right */
  animation: 0.4s ease-out both slide-in;
}

@keyframes slide-out {
  to { transform: translateX(-20%); opacity: 0; }
}

@keyframes slide-in {
  from { transform: translateX(100%); }
}
```

#### 3. Create Directional Transitions (Optional)

You may want different transition effects depending on the pages you are navigating between. For instance, when navigating from a home page to a contact page, you may want the effect of new content coming from the right. When navigating back to the home page, it wouldn't make sense to have the same effect. 

If the page you are navigating to will always have the same transition type, regardless of how you get to the page, you can specify it with `types` in the `@view-transition` rule.

```css
@media (prefers-reduced-motion: no-preference) {
  @view-transition {
    navigation: auto;
    /* Specify the types of view transitions that will always be used on this page. */
    types: previous;
  }
}
```

You can also conditionally specify transition types inside of an event listener for `pagereveal`.

```js
window.addEventListener("pagereveal", async (e) => {
  if (e.viewTransition && window.navigation?.activation) {
    // Use application-specific logic to compute a transition type
     const transitionType = yourTransitionTypeLogic(navigation.activation.from, navigation.activation.entry);
    e.viewTransition.types.add(transitionType);
  }
});
```

Then, use the `:active-view-transition-type()` pseudo selector to apply the different animations for each type.

```css
:active-view-transition-type(next) {
  &::view-transition-old(root) {
    animation-name: slide-out-next;
  }

  &::view-transition-new(root) {
    animation-name: slide-in-next;
  }
}
:active-view-transition-type(previous) {
  &::view-transition-old(root) {
    animation-name: slide-out-previous;
  }

  &::view-transition-new(root) {
    animation-name: slide-in-previous;
  }
}
```

### Fallback strategies

Baseline status for View transitions: Newly available. It's been Baseline since 2025-10-14.
Supported by: Chrome 111 (Mar 2023), Edge 111 (Mar 2023), Firefox 144 (Oct 2025), and Safari 18 (Sep 2024).

Cross-document view transitions has limited availability.
Supported by: Chrome 126 (Jun 2024), Edge 126 (Jun 2024), and Safari 18.2 (Dec 2024).
Unsupported in: Firefox.

If a browser does not support view transitions, or cross-document view transitions, it will perform a standard instant page navigation. Cross-document view transitions are a progressive enhancement; the core functionality of the site remains unaffected.

To check for support in JavaScript:

```javascript
if ('onpagereveal' in window) {
  // Browser supports cross-document view transitions
}
```

Baseline status for Navigation API: Newly available. It's been Baseline since 2026-01-13.
Supported by: Chrome 102 (May 2022), Edge 102 (May 2022), Firefox 147 (Jan 2026), and Safari 26.2 (Dec 2025).

If a browser does not support the Navigation API, you will not be able to use it to determine a transition type. Use an alternate method for determining the transition type, or provide a fallback transition type. Otherwise, the browser will perform a standard instant page navigation.

To check for support in JavaScript:

```javascript
if (window.navigation?.activation) {
  // Browser supports the Navigation API
}
```
