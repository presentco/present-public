import React, { Component, PropTypes} from 'react';
import {connect} from 'react-redux';
import {bindActionCreators} from 'redux';
import * as groupActions from '../../../../../redux/actions/groupActions';
import ApiHandler from '../../../../../core/ApiHandler';
import Constants from '../../../../../core/Constants';
import _ from 'lodash';

class Categories extends Component {

  onClickCategory(interest){
    let request = ApiHandler.attachHeader({url: `${Constants.circleUrl.url}t/${encodeURIComponent(interest.trim())}`});
    this.props.actions.getGroupInfo(request).then(response => {
      this.context.router.push(`/t/${response.name}`);
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }

  getImagePath(interest){
    let label = interest.toLowerCase(),
        link;

    if(interest.indexOf(' ') !== -1){
      label = interest.split(' ').join('').toLowerCase();
      if(label === 'womanowned'){
        label = 'woman';
      }
    }

    //this is not good will change it later
    if(label !== 'work' && label !== 'volunteer' && label !== 'health' && label !== 'exercise' && label !== 'woman' &&
    label !== 'shop' && label !== 'communities' && label !== 'attend' &&
    label !== 'eat&drink' && label !== 'friends'){
      link = require(`../../../../../assets/images/newcategory-selected@2x.png`);
    } else {
      link = require(`../../../../../assets/images/${label}-selected@2x.png`);
    }
    return link;
  }

  renderCategories(){
    let categories = <div/>;
    if(!_.isEmpty(this.props.currentGroup)){
      //check if there is categories
      if(this.props.currentGroup.categories.length !== 0){
          categories = this.props.currentGroup.categories.map((interest, index) => {

             return (
              <div className="categories-con cursor-poiner" key={index} onClick={this.onClickCategory.bind(this, interest)}>
                  <img alt="interests" className="cat-imag" src={this.getImagePath(interest)} />
                <p className="cat-name">{interest}</p>
              </div>
            )

        });
      }
    }
    return categories;
  }

  render(){
    return(
      <div className="description-container categories-container">
          <p className="title-gen">Categories</p>
          {this.renderCategories()}
      </div>
    )
  }
}

Categories.contextTypes = {
 router: PropTypes.object
}


function mapStateToProps(state, ownProps) {
    return {
    }
}

function mapDispatchToProps(dispatch) {
    return {
        actions: bindActionCreators(groupActions, dispatch)
    }
}


export default connect(mapStateToProps, mapDispatchToProps)(Categories);
