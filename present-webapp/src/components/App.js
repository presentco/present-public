//Component that Loads on every page
//and wraps all child components like a backbone.

import React, {Component} from 'react';
import { connect } from 'react-redux';
export class App extends Component {

    render() {
        return (
            <div>
                {this.props.children}
            </div>
        );
      }
}


function mapStateToProps(state) {
    return {
        loading: state.numAjaxCallsInProgress > 0
    }
}

export default connect(mapStateToProps)(App);
