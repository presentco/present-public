import React, { Component} from 'react';
import {connect} from 'react-redux';
import _ from 'lodash';

class SearchBox extends Component {
  constructor(props){
    super(props);
    this.state={
      search:"",
      result:[]
    }
    this.onChangeValue = this.onChangeValue.bind(this);
  }

  onChangeValue(e){
    e.preventDefault();
    let result = [],
        groupIds=[];

    if(this.props.nearByGroups.length !== 0){
      this.props.nearByGroups.map(group => {
          if(group.title.toLowerCase().indexOf((e.target.value.toLowerCase())) > -1){
            if(groupIds.indexOf(group.uuid) === -1){
              groupIds.push(group.uuid);
              result.push(group);
            }
          }
        });
    }

    this.setState({
      result,
      search:e.target.value
    });

    this.props.onSendQuery(this.state.result);
    this.props.searchResult(e.target.value.length !== 0 ? true : false);
  }


  render() {

    return (
      <div className="search-container">
        <input
          placeholder="Whatchya Lookin' For?"
          className="search-input"
          value={this.state.search}
          onChange={this.onChangeValue}
          autoFocus/>
        <button className="search-close-btn" onClick={() => this.props.closeModal()}>
          <img alt="close" className="close-btn-nav" src={require('../../../../assets/images/x-btn.png')} />
        </button>
      </div>
    );
  }
}


function mapStateToProps(state, ownProps) {
    return {

    }
}

function mapDispatchToProps(dispatch) {
    return {
    }
}

export default connect(mapStateToProps, mapDispatchToProps)(SearchBox);
