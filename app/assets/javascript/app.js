
function hideDetails() {
  const details = document.querySelector('.hide-details')

  if (details != null && details != 'undefined') {
    details.style.display = 'none'
  }
}

function hideAccordion() {
  const accordion = document.querySelector('.accordion')

  if (accordion != null && accordion != 'undefined') {
    const dd = accordion.getElementsByTagName('dd')

    if (dd != null && dd.length > 0) {
      for (let i = 0; i < dd.length; i++) {
        dd[i].style.display = 'none'
      }
    }
  }
}

function accordionToggleShowHide() {
  const expandables = document.getElementsByClassName('expandable')

  if (expandables != null && expandables.length > 0) {
    for (let i = 0; i < expandables.length; i++) {
      expandables[i].addEventListener('click', function (e) {
        e.preventDefault();
        if (this.getAttribute('class') !== 'active') {
          this.setAttribute('class', 'active')
          this.nextElementSibling.style.display = 'block'
          this.querySelector('.view-details').setAttribute('aria-expanded', 'true')
          this.setAttribute('aria-expanded', 'true')
        } else {
          this.removeAttribute('class')
          this.nextElementSibling.style.display = 'none'
          this.querySelector('.view-details').setAttribute('aria-expanded', 'false')
          this.setAttribute('aria-expanded', 'false')
        }
      });
    }
  }
}

function printLink() {
  const printLink = document.getElementById('printLink');

  if (printLink != null && printLink != 'undefined') {
    printLink.addEventListener('click', function (e) {
      e.preventDefault();
      window.print();
    });
  }
}

document.addEventListener('DOMContentLoaded', function (){
  hideDetails();
  hideAccordion();
  accordionToggleShowHide();
  printLink();
}, false);


