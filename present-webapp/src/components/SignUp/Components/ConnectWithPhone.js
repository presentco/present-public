import React, {Component, PropTypes} from 'react';
import * as userActions from '../../../redux/actions/userActions';
import * as groupActions from '../../../redux/actions/groupActions';
import {connect} from 'react-redux';
import {bindActionCreators} from 'redux';
import './style.css';

class ConnectWithPhone extends Component {

    constructor(props){

      super(props);
      this.state={}
    }

    // Load Appropriate Provider

    render() {

      return (
        <button className="fb-login-btn fb-small" onClick={() => this.props.onClickConnect()}>
          <p className="no-link-text">Connect with Phone Number</p>
        </button>
      )
    }
}

ConnectWithPhone.contextTypes = {
 router: PropTypes.object
}

function mapStateToProps(state, ownProps) {

  return {
      currentUser: state.user.currentUser
  }
}

function mapDispatchToProps(dispatch) {
  return {
      actions: bindActionCreators({...userActions,...groupActions}, dispatch)
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(ConnectWithPhone);
