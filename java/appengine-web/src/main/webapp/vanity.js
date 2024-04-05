// localStorage, interface stub
var cache = {};
var _localStorage = {
  getItem: function(name) {
    return cache[name];
  },
  setItem: function(name, value) {
    cache[name] = value;
  },
};



document.onreadystatechange = function () {
  var state = document.readyState;
  if (state == 'complete') init();
};

function init() {
  document.body.addEventListener('click', clickListener, true);
  document.querySelector('#beta').addEventListener('submit', formSubmit);

  var form = document.querySelector('form');
  form.addEventListener('keyup', formListener(form), false);
  var data = _localStorage.getItem('requested')
  if (data) {
    setModalIntoSentMode();
  }
}

function clickListener(ev) {
  var target = ev.target;

  if (!target) return;

  var modalTarget = target.getAttribute("data-modal");
  if (modalTarget) {
    ev.preventDefault();
    if (modalTarget === 'open') {
      openModal();
    }
    if (modalTarget === 'close') {
      closeModal();
    }
  } else if (target.className === 'modal-bg') {
    ev.preventDefault();
    closeModal();
  } else if (target.className === 'share') {
    ev.preventDefault();
    toggleShareSheet();
  }
}

function toggleShareSheet() {
  var shareSheet = document.querySelector('.share-sheet');

  if (!window.clipboardLinked) {
    window.clipboardLinked = new Clipboard('.link', {
      text: function(trigger) {
        return window.location.href;
      }
    });

    window.clipboardLinked.on('success', function(e) {
      shareSheet.querySelector('.share').innerText = 'Link Copied!';
      closeShareSheet();
    });
  }

  if (shareSheet.className.indexOf('open') >= 0) {
    shareSheet.className = removeClass(shareSheet.className, 'open');
  } else {
    shareSheet.className = addClass(shareSheet.className, 'open');
  }
}

function closeShareSheet() {
  var shareSheet = document.querySelector('.share-sheet');
  shareSheet.className = removeClass(shareSheet.className, 'open');
}

function formListener(form) {
  var formIsDisabled = true;
  var checkFormForState = function() {
    var data = getDataFromForm(form);
    if (formIsDisabled && data.length === 4) {
      formIsDisabled = false;
      setFormButton(formIsDisabled);
    } else if (!formIsDisabled && data.length !== 4) {
      formIsDisabled = true;
      setFormButton(formIsDisabled);
    }
  }

  setFormButton(formIsDisabled);
  return checkFormForState;
}

function setFormButton(formIsDisabled) {
  var submitButton = document.querySelector('form button');
  if (formIsDisabled) {
    submitButton.setAttribute('disabled', true);
  } else {
    submitButton.removeAttribute('disabled');
  }
}

function formSubmit(ev) {
  ev.preventDefault();
  var form = ev.target;
  form.className = addClass(form.className, 'loading');
  var data = getDataFromForm(form);

  if (data.length !== 4) return;

  post('/rest/requestInvitation', data.join('&'), function() {
    form.className = removeClass(form.className, 'loading');
    if (this.status === 200) {
      _localStorage.setItem('requested', data.join('&'));
      setModalIntoSentMode();
    }
  });
}
function post(url, data, cb) {
  var xhr = new XMLHttpRequest();

  xhr.open("POST", url);
  xhr.setRequestHeader("Content-type", "application/x-form-urlencoded");

  //.bind ensures that this inside of the function is the XHR object.
  xhr.onload = cb.bind(xhr);

  //All preperations are clear, send the request!
  xhr.send(data || '');
}
function getDataFromForm(form) {
  //This is a bit tricky, [].fn.call(form.elements, ...) allows us to call .fn
  //on the form's elements, even though it's not an array. Effectively
  //Filtering all of the fields on the form
  var params = [].filter.call(form.elements, function(el) {
    //Allow only elements that don't have the 'checked' property
    //Or those who have it, and it's checked for them.
    return (
      el.type !== 'checkbox' ||
      el.type !== 'radio' ||
      el.type !== 'select'
    ) &&
      !!el.name &&
      !el.disabled &&
      el.value.trim() !== '';
  })
  .map(function(el) {
      //Map each field into a name=value string, make sure to properly escape!
      return encodeURIComponent(el.name) + '=' + encodeURIComponent(el.value);
  });

  return params;
}

function openModal() {
  var modal = document.querySelector('.modal');

  fadeIn(modal, function (el) {
    modal.className = removeClass(modal.className, 'hidden');
    modal.className = addClass(modal.className, 'showing');
  });
}
function closeModal() {
  var modal = document.querySelector('.modal');
  modal.className = addClass(modal.className, 'hidden');
}
function setModalIntoSentMode() {
  var modal = document.querySelector('.modal');
  var data = _localStorage.getItem('requested')
  var name = '';

  if (data) {
   var firstName = data.split('&')
      .filter(function(item) { return item.indexOf('firstName') === 0; });

    if (firstName.length) {
      name = firstName[0].split('=').splice(-1)[0];
    }
  }

  var title = modal.querySelector('.thanks .title .name');
  title.innerText = title.innerText.replace('__NAME__', decodeURI(name));

  modal.className = addClass(modal.className, 'sent');

  var requestButton = document.querySelector('#request');
  requestButton.innerText = 'Invitation Requested';
}

function addClass(classString, newClass) {
  classString = classString.split(' ')
  classString.push(newClass);
  return classString.join(' ');
}

function removeClass(classString, classToRemove) {
  return classString
    .split(" ")
    .reduce(
      function(acc, cur, i) {
        if (cur !== classToRemove) acc.push(cur);
        return acc;
      },
      []
    )
    .join(' ');
}

function fadeIn(el, cb) {
  if (!cb) cb = function() { };
  el.style.opacity = 0;

  var last = +new Date();
  var tick = function() {
    el.style.opacity = +el.style.opacity + (new Date() - last) / 400;
    last = +new Date();

    if (+el.style.opacity < 1) {
      (window.requestAnimationFrame && requestAnimationFrame(tick)) || setTimeout(tick, 16);
    } else {
      el.style.removeProperty('opacity');
      cb(el);
    }
  };

  tick();
}
