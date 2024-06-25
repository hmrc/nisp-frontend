
function hideDetails() {
  const details = document.querySelector('.hide-details')

  if (details != null && details != 'undefined') {
    details.style.display = 'none'
  }
}

function hideAccordion() {
  const summaryList = document.getElementsByClassName('govuk-summary-list')

  if (summaryList != null && summaryList.length > 0) {
    for (let i = 0; i < summaryList.length; i++) {
      const nirecord = summaryList[i].getElementsByClassName('contributions-details')

      if (nirecord != null && nirecord.length > 0) {
        for (let i = 0; i < nirecord.length; i++) {
          nirecord[i].style.display = 'none'
        }
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
        if (this.getAttribute('class') !== 'govuk-summary-list__row active') {
          this.setAttribute('class', 'govuk-summary-list__row active')
          this.nextElementSibling.style.display = 'table-row'
          this.querySelector('.view-details').setAttribute('aria-expanded', 'true')
        } else {
          this.setAttribute('class', 'govuk-summary-list__row expandable')
          this.nextElementSibling.style.display = 'none'
          this.querySelector('.view-details').setAttribute('aria-expanded', 'false')
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

function goBack() {
  const backLink = document.getElementById('nispBackLink');

  if (backLink != null && backLink != 'undefined') {
    backLink.addEventListener('click', function (e) {
      e.preventDefault();
      window.history.back();
    });
  }
}

document.addEventListener('DOMContentLoaded', function (){
  hideDetails();
  hideAccordion();
  accordionToggleShowHide();
  printLink();
  goBack();
}, false);


