document.addEventListener('DOMContentLoaded', () => {
  const toc = document.getElementById('toc');
  if (!toc) return;

  // 1. Check the path once on load
  const isModulesPage = window.location.pathname.includes('/modules');

  const links = Array.from(toc.querySelectorAll('a'));
  const sections = [];

  links.forEach(link => {
    const href = link.getAttribute('href');
    if (href && href.startsWith('#')) {
      const id = href.substring(1);
      const target = document.getElementById(id);
      if (target) {
        sections.push({ link, target });
      }
    }
  });

  if (sections.length === 0) return;

  function updateToc() {
    const offset = 95;
    let activeIndex = -1;

    for (let i = 0; i < sections.length; i++) {
      const bounds = sections[i].target.getBoundingClientRect();
      if (bounds.top <= offset) {
        activeIndex = i;
      } else {
        break;
      }
    }

    const scrollY = window.scrollY || window.pageYOffset;

    if ((window.innerHeight + scrollY) >= document.documentElement.scrollHeight - 10) {
      activeIndex = sections.length - 1;
    }

    if (scrollY < 50) {
      activeIndex = -1;
    }

    // 1. Reset all states completely
    links.forEach(l => {
      l.classList.remove('active');
      let p = l.parentElement;
      while (p && p !== toc) {
        if (p.tagName === 'LI') p.classList.remove('expanded');
        p = p.parentElement;
      }
    });

    // 2. Apply the active path based on scroll position
    if (activeIndex !== -1) {
      const activeLink = sections[activeIndex].link;
      activeLink.classList.add('active');
      let p = activeLink.parentElement;
      while (p && p !== toc) {
        if (p.tagName === 'LI') p.classList.add('expanded');
        p = p.parentElement;
      }
    }

    // If we are on the modules page, ensure the first layer is ALWAYS expanded,
    // regardless of where we are scrolled.
    if (isModulesPage) {
      const topLevelLis = toc.querySelectorAll('.sectlevel1 > li');
      topLevelLis.forEach(li => li.classList.add('expanded'));
    }
  }

  window.addEventListener('scroll', updateToc, { passive: true });
  setTimeout(updateToc, 100);
});
