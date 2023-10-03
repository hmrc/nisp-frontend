
function hideDetails() {
  const details =
      document
          .querySelector('.hide-details')

  if (details != null) {
    details.style.display = "none"
  }
}

function hideAccordion() {
  const dd =
      document
          .querySelector('.accordion')
          .getElementsByTagName('dd')

  if (dd != null && dd.length > 0) {
    for (let i = 0; i < dd.length; i++) {
      dd[i].style.display = "none"
    }
  }
}

function accordionShowHide() {
  const expandables =
      document
          .getElementsByClassName('expandable')

  if (expandables != null && expandables.length > 0) {
    console.log(expandables.length)
    for (let i = 0; i < expandables.length; i++) {
      let expandable = expandables[i]
      expandable.onclick = function (e) {
        e.preventDefault();
        if (this.getAttribute('class') !== 'active') {
          this.setAttribute('class', 'active')
          this.nextElementSibling.style.display = "block"
          this.querySelector('.view-details').setAttribute('aria-expanded', 'true')
          this.setAttribute('aria-expanded', 'true')
        } else {
          this.removeAttribute('class')
          this.nextElementSibling.style.display = "none"
          this.querySelector('.view-details').setAttribute('aria-expanded', 'false')
          this.setAttribute('aria-expanded', 'false')
        }
      }
    }
  }
}

document.addEventListener('DOMContentLoaded', function (){
  hideDetails();
  hideAccordion();
  accordionShowHide();
}, false);

const printlink = document.getElementById('printLink');

if(printlink != null && printlink != 'undefined' ) {

  printlink.addEventListener("click", function (e) {
    e.preventDefault();
    window.print();
  });
}
