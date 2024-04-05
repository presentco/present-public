import bugsnag from 'bugsnag-js'
const bugsnagClient = bugsnag('3115460d22d7a4a5decc4f13e1cc57ea')
import React from 'react';
import ReactDOM from 'react-dom';
import createPlugin from 'bugsnag-react';
import {browserHistory} from 'react-router';
import {Provider} from 'react-redux';
import configureStore from './redux/store/configureStore';
import Routes from './routes';
import Constants from './core/Constants';
import './index.css';
import 'foundation-sites/css/foundation.min.css';
import 'foundation-sites/css/normalize.min.css';
import 'cropperjs/dist/cropper.css';
import "slick-carousel/slick/slick.css";
import "slick-carousel/slick/slick-theme.css";
import Amplitude from 'react-amplitude';

const store = configureStore();
Amplitude.initialize(Constants.Keys.Amplitude.API_ID);
const ErrorBoundary = bugsnagClient.use(createPlugin(React));


  ReactDOM.render(
      <ErrorBoundary>
        <Provider store={store}>
          <Routes history={browserHistory} />
        </Provider>
      </ErrorBoundary>,
    document.getElementById('root')
  );
