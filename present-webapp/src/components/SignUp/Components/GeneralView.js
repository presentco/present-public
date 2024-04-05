import React, { Component} from 'react';
import './style.css';
import _ from 'lodash';

class GeneralView extends Component {
  constructor(props){
    super(props);
    this.state={
      loaded:false
    }
  }

  componentDidMount(){
    this.setState({change:true});
  }

  componentWillUnmount() {
    clearTimeout();
  }

  render() {

    return(
      <div className="pic-container">
          <img alt="logo"
            src={require('../../../assets/images/discover.svg')}
            className="loaded on-phone" />
          <img alt="logo"
            src={require('../../../assets/images/phone.svg')}
            className="splash-mobile"/>
      </div>

    )
  }
}

export default GeneralView;
